package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.registry.ModItems;
import com.example.examplemod.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 도박사: 코인(만료·공격력 ±15%) + 블랙잭 카드(누적 21 = 폭발) + 룰렛 궁극기(돔 연출)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class GamblerEvents {

    public static final int COIN_TICKS = 400;        // 코인 유지 20초
    private static final float COIN_BONUS = 0.15F;   // 코인 1개당 공격력 ±15%

    public static final int CARD_TICKS = 400;             // 카드 유지 20초
    private static final int BLACKJACK_TARGET = 21;       // 목표 합
    private static final float BLACKJACK_DAMAGE = 12.0F;  // 블랙잭 보너스 데미지 (하트 6개)

    private static final int ROULETTE_TOTAL_TICKS = 80;        // 연출 4초
    private static final int DOME_RADIUS = 10;                 // 돔 반경(칸)
    private static final double FACE_HALF_GAP = 4.0;           // 중심에서 각자 이만큼 떨어짐 (서로 8칸 거리)
    private static final int ROULETTE_RESULT_TICKS = 600;      // 버프/디버프 30초
    private static final float ROULETTE_BUFF_MULT = 1.4F;      // 7 우세: 내 공격력 +40%
    private static final float ROULETTE_DEBUFF_MULT = 0.5F;    // 해골 우세: 상대 공격력 -50%

    // ==================== 매 틱 (PlayerTickHub가 호출) ====================

    public static void tick(ServerPlayer player, ServerLevel level, PlayerCombatData data) {
        // 1) 코인: 각자 남은 시간 감소, 다 되면 슬롯 비움
        boolean changed = false;
        for (int i = 0; i < 3; i++) {
            if (data.getCoinType(i) == CoinType.NONE) {
                continue;
            }
            int left = data.getCoinTicks(i) - 1;
            if (left <= 0) {
                data.setCoin(i, CoinType.NONE, 0);
                changed = true;
            } else {
                data.setCoinTicks(i, left);
            }
        }
        if (changed) {
            CombatSync.syncStats(player);
            syncCoinEffects(player, data);
        } else {
            // 코인은 있는데 아이콘이 없으면 복구 (상태초기화 등으로 효과만 지워진 경우)
            boolean hasAnyCoin = data.getCoinType(0) != CoinType.NONE
                    || data.getCoinType(1) != CoinType.NONE
                    || data.getCoinType(2) != CoinType.NONE;
            if (hasAnyCoin
                    && !player.hasEffect(com.example.examplemod.registry.ModEffects.COIN_SEVEN.get())
                    && !player.hasEffect(com.example.examplemod.registry.ModEffects.COIN_SKULL.get())) {
                syncCoinEffects(player, data);
            }
        }

        // 2) 블랙잭 카드: 시간 감소 (표시는 시전자 클라이언트가 그림 — CardMarkRenderEvents)
        int cardTicks = data.getCardTicks();
        if (cardTicks > 0) {
            Entity target = level.getEntity(data.getCardTargetId());
            if (target == null || !target.isAlive()) {
                clearCard(player, data);
            } else {
                data.setCardTicks(cardTicks - 1);
                if (cardTicks - 1 <= 0) {
                    clearCard(player, data);
                }
            }
        }

        // 3) 룰렛 연출 진행
        if (data.getRouletteTicks() > 0) {
            tickRoulette(player, level, data);
        }

        // 4) 룰렛 결과 버프/디버프: 시간 감소 + 지속되는 동안 파티클이 계속 뿜어져 나옴
        if (data.getRouletteBuffTicks() > 0) {
            int left = data.getRouletteBuffTicks() - 1;
            data.setRouletteBuffTicks(left);
            if (left % 5 == 0) {
                // 버프 중: 내 주변에서 7이 계속 피어오름
                level.sendParticles(ModParticles.SEVEN.get(),
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        3, 0.5, 0.6, 0.5, 0.02);
            }
        }
        if (data.getRouletteDebuffTicks() > 0) {
            int left = data.getRouletteDebuffTicks() - 1;
            data.setRouletteDebuffTicks(left);
            if (left <= 0) {
                data.setRouletteDebuffTargetId(-1);
            } else if (left % 5 == 0) {
                // 디버프 중: 상대 주변에서 해골이 계속 피어오름
                Entity debuffed = level.getEntity(data.getRouletteDebuffTargetId());
                if (debuffed != null && debuffed.isAlive()) {
                    level.sendParticles(ModParticles.SKULL.get(),
                            debuffed.getX(), debuffed.getY() + 1.0, debuffed.getZ(),
                            3, 0.5, 0.6, 0.5, 0.02);
                }
            }
        }
    }

    // ==================== 룰렛 궁극기 ====================

    // 시전: 둘을 마주보게 배치 → 돔 설치 → 릴 3개 스폰 → 연출 시작
    public static void startRoulette(ServerPlayer caster, ServerLevel level, LivingEntity target) {
        caster.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            Vec3 cPos = caster.position();
            Vec3 tPos = target.position();

            // 수평 방향(시전자→상대). 겹쳐 있으면 임의 방향
            Vec3 dir = new Vec3(tPos.x - cPos.x, 0, tPos.z - cPos.z);
            dir = dir.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : dir.normalize();
            double y = cPos.y;
            Vec3 center = new Vec3((cPos.x + tPos.x) / 2.0, y, (cPos.z + tPos.z) / 2.0);

            // 서로 마주보게 강제 배치 (시선 포함)
            // 배치 지점이 벽 안이면 중심 쪽으로 당겨 안전한 자리를 찾음 (벽뚫 방지)
            float casterYaw = yawFromDir(dir);
            float targetYaw = yawFromDir(dir.scale(-1));
            Vec3 casterSpot = findSafeSpot(level, caster,
                    new Vec3(center.x - dir.x * FACE_HALF_GAP, y, center.z - dir.z * FACE_HALF_GAP), center);
            Vec3 targetSpot = findSafeSpot(level, target,
                    new Vec3(center.x + dir.x * FACE_HALF_GAP, y, center.z + dir.z * FACE_HALF_GAP), center);
            caster.teleportTo(level, casterSpot.x, casterSpot.y, casterSpot.z, casterYaw, 0.0F);
            if (target instanceof ServerPlayer tp) {
                tp.teleportTo(level, targetSpot.x, targetSpot.y, targetSpot.z, targetYaw, 0.0F);
            } else {
                target.teleportTo(targetSpot.x, targetSpot.y, targetSpot.z);
                faceTowards(target, caster);
            }

            // 연출 중 무적 (제3자 개입 방지)
            caster.setInvulnerable(true);
            target.setInvulnerable(true);

            // 검은 유리 돔 (공기인 칸만 채우고 위치 기록 → 끝나면 복원)
            Vec3 domeCenter = center.add(0, 1, 0);
            buildDome(level, domeCenter, data);

            // 제3자는 돔 밖으로 밀어냄 (둘만의 결계)
            for (LivingEntity bystander : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(domeCenter, domeCenter).inflate(DOME_RADIUS + 0.5),
                    e -> e != caster && e != target && e.isAlive())) {
                Vec3 out = new Vec3(bystander.getX() - center.x, 0, bystander.getZ() - center.z);
                out = out.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : out.normalize();
                double px = center.x + out.x * (DOME_RADIUS + 2);
                double pz = center.z + out.z * (DOME_RADIUS + 2);
                // 밀려날 자리가 벽 안이면 위로 2칸까지 올려보고, 다 막혔으면 옮기지 않음 (벽뚫 방지)
                double safeY = Double.NaN;
                for (int dy = 0; dy <= 2; dy++) {
                    if (isFree(level, bystander, px, bystander.getY() + dy, pz)) {
                        safeY = bystander.getY() + dy;
                        break;
                    }
                }
                if (Double.isNaN(safeY)) {
                    continue;
                }
                if (bystander instanceof ServerPlayer sp) {
                    sp.teleportTo(level, px, safeY, pz, sp.getYRot(), sp.getXRot());
                } else {
                    bystander.teleportTo(px, safeY, pz);
                }
            }

            // 릴 3개: 마주보는 축의 수직 방향으로 나란히, 공중에
            Vec3 perp = new Vec3(-dir.z, 0, dir.x);
            for (int i = 0; i < 3; i++) {
                double off = (i - 1) * 1.4;   // 릴 간격 (공간 커진 만큼 살짝 벌림)
                Vec3 pos = new Vec3(center.x + perp.x * off, y + 2.5, center.z + perp.z * off);
                data.setRouletteDisplayId(i, spawnTextDisplay(level, pos, spinJson()));
                data.setRouletteReel(i, CoinType.NONE);
            }

            // 고정 좌표 기록 (연출 중 매 틱 이 자리로 스냅) — 실제 배치된 안전 좌표를 기록
            double[] anchors = data.getRouletteAnchors();
            anchors[0] = casterSpot.x;
            anchors[1] = casterSpot.y;
            anchors[2] = casterSpot.z;
            anchors[3] = targetSpot.x;
            anchors[4] = targetSpot.y;
            anchors[5] = targetSpot.z;

            data.setRoulettePartnerId(target.getId());
            data.setRouletteTicks(ROULETTE_TOTAL_TICKS);
        });
    }

    private static void tickRoulette(ServerPlayer caster, ServerLevel level, PlayerCombatData data) {
        Entity partnerEntity = level.getEntity(data.getRoulettePartnerId());
        if (!(partnerEntity instanceof LivingEntity partner) || !partnerEntity.isAlive()) {
            cleanupRoulette(caster, level, data);   // 상대 소멸 → 즉시 정리
            return;
        }

        // 둘 다 완전 고정 (움직이려 해도 제자리로 스냅)
        double[] anchors = data.getRouletteAnchors();
        pinTo(level, caster, anchors[0], anchors[1], anchors[2]);
        pinTo(level, partner, anchors[3], anchors[4], anchors[5]);
        if (!(partner instanceof ServerPlayer)) {
            faceTowards(partner, caster);   // 몹은 계속 시전자를 바라보게
        }

        int next = data.getRouletteTicks() - 1;
        data.setRouletteTicks(next);

        // 릴이 차례로 멈춤 (1초 간격)
        if (next == 60) {
            lockReel(level, data, 0, caster);
        } else if (next == 40) {
            lockReel(level, data, 1, caster);
        } else if (next == 20) {
            lockReel(level, data, 2, caster);
        } else if (next % 2 == 0) {
            // 아직 안 멈춘 릴은 빠르게 회전 (2틱마다 심볼 교체)
            for (int i = 0; i < 3; i++) {
                if (data.getRouletteReel(i) == CoinType.NONE) {
                    setDisplayText(level, data.getRouletteDisplayId(i), spinJson());
                }
            }
        }

        if (next == 10) {
            applyRouletteResult(caster, level, data, partner);   // 판정 + 이펙트
        }
        if (next <= 0) {
            cleanupRoulette(caster, level, data);
        }
    }

    private static void lockReel(ServerLevel level, PlayerCombatData data, int i, ServerPlayer caster) {
        CoinType result = caster.getRandom().nextBoolean() ? CoinType.SEVEN : CoinType.SKULL;
        data.setRouletteReel(i, result);
        String json = result == CoinType.SEVEN
                ? "{\"text\":\"7\",\"color\":\"gold\",\"bold\":true}"
                : "{\"text\":\"" + SKULL_GLYPH + "\",\"color\":\"red\",\"bold\":true}";
        setDisplayText(level, data.getRouletteDisplayId(i), json);
    }

    private static void applyRouletteResult(ServerPlayer caster, ServerLevel level,
                                            PlayerCombatData data, LivingEntity partner) {
        int sevens = 0;
        for (int i = 0; i < 3; i++) {
            if (data.getRouletteReel(i) == CoinType.SEVEN) {
                sevens++;
            }
        }

        if (sevens >= 2) {
            // 7 우세: 내 공격력 +40% (30초) — 발표 순간 내 주변에서 7이 크게 뿜어짐
            data.setRouletteBuffTicks(ROULETTE_RESULT_TICKS);
            caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.example.examplemod.registry.ModEffects.JACKPOT.get(),
                    ROULETTE_RESULT_TICKS, 0, false, false, true));
            Vec3 c = caster.position();
            level.sendParticles(ModParticles.SEVEN.get(), c.x, c.y + 1.0, c.z, 40, 0.8, 0.8, 0.8, 0.15);
        } else {
            // 해골 우세: 상대 공격력 -50% (30초) — 발표 순간 상대 주변에서 해골이 크게 뿜어짐
            data.setRouletteDebuffTargetId(partner.getId());
            data.setRouletteDebuffTicks(ROULETTE_RESULT_TICKS);
            partner.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.example.examplemod.registry.ModEffects.CURSED.get(),
                    ROULETTE_RESULT_TICKS, 0, false, false, true));
            Vec3 c = partner.position();
            level.sendParticles(ModParticles.SKULL.get(), c.x, c.y + 1.0, c.z, 40, 0.8, 0.8, 0.8, 0.15);
        }
    }

    // 코인 상태 → HUD 효과 아이콘 동기화 (개수 = 효과 레벨, 지속 = 가장 오래 남은 코인)
    public static void syncCoinEffects(ServerPlayer player, PlayerCombatData data) {
        int sevens = 0, skulls = 0, sevenTicks = 0, skullTicks = 0;
        for (int i = 0; i < 3; i++) {
            if (data.getCoinType(i) == CoinType.SEVEN) {
                sevens++;
                sevenTicks = Math.max(sevenTicks, data.getCoinTicks(i));
            } else if (data.getCoinType(i) == CoinType.SKULL) {
                skulls++;
                skullTicks = Math.max(skullTicks, data.getCoinTicks(i));
            }
        }
        // 스택이 줄 땐 낮은 레벨이 높은 레벨을 못 덮는 바닐라 규칙 때문에 항상 지우고 다시 적용
        player.removeEffect(com.example.examplemod.registry.ModEffects.COIN_SEVEN.get());
        if (sevens > 0) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.example.examplemod.registry.ModEffects.COIN_SEVEN.get(),
                    sevenTicks, sevens - 1, false, false, true));
        }
        player.removeEffect(com.example.examplemod.registry.ModEffects.COIN_SKULL.get());
        if (skulls > 0) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.example.examplemod.registry.ModEffects.COIN_SKULL.get(),
                    skullTicks, skulls - 1, false, false, true));
        }
    }

    // 연출 종료/중단: 표시 제거 + 돔 복원 + 무적 해제
    private static void cleanupRoulette(ServerPlayer caster, ServerLevel level, PlayerCombatData data) {
        for (int i = 0; i < 3; i++) {
            Entity display = level.getEntity(data.getRouletteDisplayId(i));
            if (display != null) {
                display.discard();
            }
            data.setRouletteDisplayId(i, -1);
            data.setRouletteReel(i, CoinType.NONE);
        }

        for (BlockPos pos : data.getRouletteDomeBlocks()) {
            if (level.getBlockState(pos).is(Blocks.BLACK_STAINED_GLASS)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);   // 원래 공기였던 칸만 되돌림
            }
        }
        data.getRouletteDomeBlocks().clear();

        caster.setInvulnerable(false);
        Entity partner = level.getEntity(data.getRoulettePartnerId());
        if (partner != null) {
            partner.setInvulnerable(false);
        }

        data.setRoulettePartnerId(-1);
        data.setRouletteTicks(0);
    }

    // 시전자가 죽거나 나가면 연출 강제 정리 (돔/무적/카드 표시가 남지 않게)
    public static void onCasterGone(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
                if (data.getRouletteTicks() > 0) {
                    cleanupRoulette(player, level, data);
                }
                if (data.getCardTicks() > 0) {
                    clearCard(player, data);   // 죽으면 카드 데이터가 초기화돼 표시가 영영 남던 버그 방지
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onCasterGone(player);
        }
    }

    // 로그아웃 시에도 즉시 정리 (카드·룰렛 표시가 월드에 남지 않게)
    @SubscribeEvent
    public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onCasterGone(player);
        }
    }

    // 돔 블럭은 캘 수 없음 (진행 중인 룰렛의 돔인 경우만)
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isDomeBlock(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    // 폭발로도 안 부서짐
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            event.getAffectedBlocks().removeIf(pos -> isDomeBlock(level, pos));
        }
    }

    private static boolean isDomeBlock(ServerLevel level, BlockPos pos) {
        for (ServerPlayer p : level.players()) {
            boolean hit = p.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                    .map(d -> d.getRouletteTicks() > 0 && d.getRouletteDomeBlocks().contains(pos))
                    .orElse(false);
            if (hit) {
                return true;
            }
        }
        return false;
    }

    // ----- 룰렛 보조 -----

    private static void buildDome(ServerLevel level, Vec3 center, PlayerCombatData data) {
        BlockPos c = BlockPos.containing(center);
        for (int dx = -DOME_RADIUS; dx <= DOME_RADIUS; dx++) {
            for (int dy = -DOME_RADIUS; dy <= DOME_RADIUS; dy++) {
                for (int dz = -DOME_RADIUS; dz <= DOME_RADIUS; dz++) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist < DOME_RADIUS - 0.5 || dist > DOME_RADIUS + 0.5) {
                        continue;   // 껍질만
                    }
                    BlockPos pos = c.offset(dx, dy, dz);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.BLACK_STAINED_GLASS.defaultBlockState(), 3);
                        data.getRouletteDomeBlocks().add(pos.immutable());
                    }
                }
            }
        }
    }

    // 완전 고정: 지정 좌표에서 벗어나면 되돌리고 속도도 죽임 (시점은 자유)
    // 그 좌표에 서 있어도 블록과 겹치지 않는지 (히트박스 기준)
    private static boolean isFree(ServerLevel level, LivingEntity entity, double x, double y, double z) {
        AABB box = entity.getDimensions(entity.getPose()).makeBoundingBox(x, y, z);
        return level.noCollision(entity, box);
    }

    // 원하는 배치 지점이 벽 안이면 중심 쪽으로 0.5칸씩 당기고(중심 1칸 앞까지),
    // 각 지점마다 위로 2칸까지 올려본다. 전부 막혀 있으면 원래 서 있던 자리 그대로.
    private static Vec3 findSafeSpot(ServerLevel level, LivingEntity entity, Vec3 desired, Vec3 center) {
        Vec3 toCenter = new Vec3(center.x - desired.x, 0, center.z - desired.z);
        double dist = toCenter.length();
        Vec3 step = dist < 0.01 ? Vec3.ZERO : toCenter.scale(1.0 / dist);
        double maxPull = Math.max(0, dist - 1.0);
        for (double pull = 0; pull <= maxPull; pull += 0.5) {
            double px = desired.x + step.x * pull;
            double pz = desired.z + step.z * pull;
            for (int dy = 0; dy <= 2; dy++) {
                if (isFree(level, entity, px, desired.y + dy, pz)) {
                    return new Vec3(px, desired.y + dy, pz);
                }
            }
        }
        return entity.position();
    }

    private static void pinTo(ServerLevel level, LivingEntity e, double x, double y, double z) {
        if (e.distanceToSqr(x, y, z) > 0.01) {
            if (e instanceof ServerPlayer sp) {
                sp.teleportTo(level, x, y, z, sp.getYRot(), sp.getXRot());
            } else {
                e.teleportTo(x, y, z);
            }
        }
        Vec3 v = e.getDeltaMovement();
        e.setDeltaMovement(0.0, Math.min(v.y, 0.0), 0.0);
        e.hurtMarked = true;
    }

    // 룰렛 연출에 붙잡혀 있는지 (시전자 본인 or 누군가의 상대) — 이동·공격 차단용
    public static boolean isRouletteLocked(Entity e) {
        if (!(e.level() instanceof ServerLevel level)) {
            return false;
        }
        if (e instanceof ServerPlayer sp) {
            boolean self = sp.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                    .map(d -> d.getRouletteTicks() > 0)
                    .orElse(false);
            if (self) {
                return true;
            }
        }
        for (ServerPlayer p : level.players()) {
            boolean partner = p.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                    .map(d -> d.getRouletteTicks() > 0 && d.getRoulettePartnerId() == e.getId())
                    .orElse(false);
            if (partner) {
                return true;
            }
        }
        return false;
    }

    private static void faceTowards(LivingEntity e, LivingEntity other) {
        double dx = other.getX() - e.getX();
        double dz = other.getZ() - e.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        e.setYRot(yaw);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
    }

    private static float yawFromDir(Vec3 dir) {
        return (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
    }

    // 해골 기호 ☠ (마크 기본 글꼴의 유니코드 그림 글자) — 예전엔 "해골"이라는 글자가 그대로 떴음
    private static final String SKULL_GLYPH = "☠";

    private static String spinJson() {
        // 회전 중 심볼 (회색으로 7/해골이 빠르게 교차)
        return Math.random() < 0.5
                ? "{\"text\":\"7\",\"color\":\"gray\"}"
                : "{\"text\":\"" + SKULL_GLYPH + "\",\"color\":\"gray\"}";
    }

    // 릴 표시 엔티티를 그대로 둔 채 글자만 교체 (예전엔 2틱마다 지우고 새로 만들어 깜빡임·엔티티 낭비).
    // 표시 엔티티의 글자 설정 함수가 비공개라, 저장 데이터를 꺼내 글자만 바꿔 다시 불러오는 방식
    private static void setDisplayText(ServerLevel level, int id, String json) {
        Entity display = level.getEntity(id);
        if (display == null) {
            return;
        }
        CompoundTag tag = display.saveWithoutId(new CompoundTag());
        tag.putString("text", json);
        display.load(tag);
    }

    private static int spawnTextDisplay(ServerLevel level, Vec3 pos, String json) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:text_display");
        tag.putString("text", json);
        tag.putString("billboard", "center");
        Entity display = EntityType.loadEntityRecursive(tag, level, e -> {
            e.setPos(pos.x, pos.y, pos.z);
            return e;
        });
        if (display != null) {
            // 안전망: 어떤 경로로든 제거를 놓쳐도 카드 수명 + 여유 뒤엔 청소부가 소멸시킴
            TempDisplaySweeper.mark(display, level.getGameTime() + CARD_TICKS + 60);
            level.addFreshEntity(display);
            return display.getId();
        }
        return -1;
    }

    // ==================== 블랙잭 카드 ====================

    // 카드 투사체 명중: 그 적에게 카드 부착 (표시는 시전자 화면에만 — CardMarkS2CPacket)
    public static void attachCard(ServerPlayer shooter, LivingEntity target) {
        shooter.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            data.setCardTargetId(target.getId());
            data.setCardSum(0);
            data.setCardTicks(CARD_TICKS);
            com.example.examplemod.network.ModMessages.sendToPlayer(
                    new com.example.examplemod.network.CardMarkS2CPacket(target.getId(), 0, CARD_TICKS), shooter);
        });
    }

    // 총알 명중: 카드 대상이면 "액면가(굴린 정수)"를 누적 — 코인 버프와 무관하게 항상 정수
    public static void onBulletHit(ServerPlayer shooter, LivingEntity target, int faceValue) {
        if (!(shooter.level() instanceof ServerLevel level)) {
            return;
        }
        shooter.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            if (data.getCardTicks() <= 0 || data.getCardTargetId() != target.getId()) {
                return;
            }

            int sum = data.getCardSum() + faceValue;
            if (sum == BLACKJACK_TARGET) {
                // 💥 블랙잭! (직전 총알의 무적시간을 풀어 보너스가 온전히 들어가게)
                target.invulnerableTime = 0;
                target.hurt(shooter.damageSources().playerAttack(shooter), BLACKJACK_DAMAGE);
                level.sendParticles(ParticleTypes.EXPLOSION,
                        target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                        3, 0.3, 0.3, 0.3, 0.0);
                // 해골이 사방으로 확! 터져 나감 (count=0 = 오프셋이 속도 방향, 감속 0.95라 퍼졌다 멈춤)
                for (int i = 0; i < 22; i++) {
                    double yaw = shooter.getRandom().nextDouble() * Math.PI * 2;
                    double pitch = (shooter.getRandom().nextDouble() - 0.35) * Math.PI * 0.7;
                    double dx = Math.cos(yaw) * Math.cos(pitch);
                    double dz = Math.sin(yaw) * Math.cos(pitch);
                    double dy = Math.sin(pitch) + 0.2;
                    level.sendParticles(ModParticles.SKULL.get(),
                            target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                            0, dx, dy, dz, 0.13);   // 감속 감안 도달거리 약 2칸
                }
                clearCard(shooter, data);
            } else if (sum > BLACKJACK_TARGET) {
                // 버스트 → 소멸 대신 초과분 이월 (예: 19+5=24 → 3부터 다시). 카드는 계속 유지
                int carry = sum - BLACKJACK_TARGET;
                data.setCardSum(carry);
                com.example.examplemod.network.ModMessages.sendToPlayer(
                        new com.example.examplemod.network.CardMarkS2CPacket(
                                target.getId(), carry, data.getCardTicks()), shooter);
                level.sendParticles(ParticleTypes.SMOKE,
                        target.getX(), target.getY() + target.getBbHeight() + 0.6, target.getZ(),
                        6, 0.2, 0.2, 0.2, 0.02);
                shooter.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("버스트! 초과분 이월: " + carry), true);
            } else {
                data.setCardSum(sum);
                com.example.examplemod.network.ModMessages.sendToPlayer(
                        new com.example.examplemod.network.CardMarkS2CPacket(
                                target.getId(), sum, data.getCardTicks()), shooter);
            }
        });
    }

    // 머리 위 "n / 21" 표시 생성
    // 카드 상태 정리 + 시전자 화면 표시 제거
    private static void clearCard(ServerPlayer caster, PlayerCombatData data) {
        if (data.getCardTargetId() != -1) {
            com.example.examplemod.network.ModMessages.sendToPlayer(
                    new com.example.examplemod.network.CardMarkS2CPacket(data.getCardTargetId(), 0, 0), caster);
        }
        data.setCardTargetId(-1);
        data.setCardSum(0);
        data.setCardTicks(0);
    }

    // ==================== 데미지 보정 (코인 ±15% / 룰렛 +40% / 룰렛 -50%) ====================

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity src = event.getSource().getEntity();

        // 룰렛 연출에 붙잡힌 참가자는 아무도 공격 불가
        if (src != null && isRouletteLocked(src)) {
            event.setCanceled(true);
            return;
        }

        // 공격자가 도박사: 코인 ±15% + 룰렛 7 버프 +40%
        if (src instanceof ServerPlayer attacker) {
            attacker.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
                int net = 0;
                for (int i = 0; i < 3; i++) {
                    if (data.getCoinType(i) == CoinType.SEVEN) {
                        net++;
                    } else if (data.getCoinType(i) == CoinType.SKULL) {
                        net--;
                    }
                }
                float mult = 1.0F;
                if (net != 0) {
                    mult *= (1.0F + COIN_BONUS * net);
                }
                if (data.getRouletteBuffTicks() > 0) {
                    mult *= ROULETTE_BUFF_MULT;
                }
                if (mult != 1.0F) {
                    event.setAmount(event.getAmount() * mult);
                }
            });
        }

        // 공격자가 누군가의 룰렛 해골 디버프 대상이면: 주는 데미지 -50%
        if (src != null && src.level() instanceof ServerLevel level) {
            for (ServerPlayer p : level.players()) {
                boolean debuffed = p.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                        .map(d -> d.getRouletteDebuffTicks() > 0
                                && d.getRouletteDebuffTargetId() == src.getId())
                        .orElse(false);
                if (debuffed) {
                    event.setAmount(event.getAmount() * ROULETTE_DEBUFF_MULT);
                    break;
                }
            }
        }
    }
}
