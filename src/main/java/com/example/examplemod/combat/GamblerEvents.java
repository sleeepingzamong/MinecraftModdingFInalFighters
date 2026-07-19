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
    private static final float BLACKJACK_DAMAGE = 15.0F;  // 블랙잭 보너스 데미지 (하트 7.5개)

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
        }

        // 2) 블랙잭 카드: 시간 감소 + 머리 위 숫자를 대상 위치로 이동
        int cardTicks = data.getCardTicks();
        if (cardTicks > 0) {
            Entity target = level.getEntity(data.getCardTargetId());
            if (target == null || !target.isAlive()) {
                clearCard(level, data);
            } else {
                Entity display = level.getEntity(data.getCardDisplayId());
                if (display != null) {
                    display.setPos(target.getX(), target.getY() + target.getBbHeight() + 0.6, target.getZ());
                }
                Entity model = level.getEntity(data.getCardModelDisplayId());
                if (model != null) {
                    // blackjack 카드: 적 중심으로 "내(시전자) 쪽" 방향에 떠 있음 → 내가 돌면 같이 공전
                    Vec3 pos = cardModelPos(player, target);
                    model.setPos(pos.x, pos.y, pos.z);
                }
                data.setCardTicks(cardTicks - 1);
                if (cardTicks - 1 <= 0) {
                    clearCard(level, data);
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

            // 서로 4칸 거리로 마주보게 강제 배치 (시선 포함)
            float casterYaw = yawFromDir(dir);
            float targetYaw = yawFromDir(dir.scale(-1));
            caster.teleportTo(level, center.x - dir.x * FACE_HALF_GAP, y, center.z - dir.z * FACE_HALF_GAP, casterYaw, 0.0F);
            if (target instanceof ServerPlayer tp) {
                tp.teleportTo(level, center.x + dir.x * FACE_HALF_GAP, y, center.z + dir.z * FACE_HALF_GAP, targetYaw, 0.0F);
            } else {
                target.teleportTo(center.x + dir.x * FACE_HALF_GAP, y, center.z + dir.z * FACE_HALF_GAP);
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
                if (bystander instanceof ServerPlayer sp) {
                    sp.teleportTo(level, px, bystander.getY(), pz, sp.getYRot(), sp.getXRot());
                } else {
                    bystander.teleportTo(px, bystander.getY(), pz);
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

            // 고정 좌표 기록 (연출 중 매 틱 이 자리로 스냅)
            double[] anchors = data.getRouletteAnchors();
            anchors[0] = center.x - dir.x * FACE_HALF_GAP;
            anchors[1] = y;
            anchors[2] = center.z - dir.z * FACE_HALF_GAP;
            anchors[3] = center.x + dir.x * FACE_HALF_GAP;
            anchors[4] = y;
            anchors[5] = center.z + dir.z * FACE_HALF_GAP;

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
                    data.setRouletteDisplayId(i, respawnDisplay(level, data.getRouletteDisplayId(i), spinJson()));
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
                : "{\"text\":\"해골\",\"color\":\"red\",\"bold\":true}";
        data.setRouletteDisplayId(i, respawnDisplay(level, data.getRouletteDisplayId(i), json));
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
            Vec3 c = caster.position();
            level.sendParticles(ModParticles.SEVEN.get(), c.x, c.y + 1.0, c.z, 40, 0.8, 0.8, 0.8, 0.15);
        } else {
            // 해골 우세: 상대 공격력 -50% (30초) — 발표 순간 상대 주변에서 해골이 크게 뿜어짐
            data.setRouletteDebuffTargetId(partner.getId());
            data.setRouletteDebuffTicks(ROULETTE_RESULT_TICKS);
            Vec3 c = partner.position();
            level.sendParticles(ModParticles.SKULL.get(), c.x, c.y + 1.0, c.z, 40, 0.8, 0.8, 0.8, 0.15);
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

    // 시전자가 죽거나 나가면 연출 강제 정리 (돔/무적이 남지 않게)
    public static void onCasterGone(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
                if (data.getRouletteTicks() > 0) {
                    cleanupRoulette(player, level, data);
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

    private static String spinJson() {
        // 회전 중 심볼 (회색으로 7/해골이 빠르게 교차)
        return Math.random() < 0.5
                ? "{\"text\":\"7\",\"color\":\"gray\"}"
                : "{\"text\":\"해골\",\"color\":\"gray\"}";
    }

    private static int respawnDisplay(ServerLevel level, int oldId, String json) {
        Entity old = level.getEntity(oldId);
        if (old == null) {
            return oldId;
        }
        Vec3 pos = old.position();
        old.discard();
        return spawnTextDisplay(level, pos, json);
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
            level.addFreshEntity(display);
            return display.getId();
        }
        return -1;
    }

    // ==================== 블랙잭 카드 ====================

    // 카드 투사체 명중: 그 적에게 카드 부착 (기존 카드는 회수)
    public static void attachCard(ServerPlayer shooter, LivingEntity target) {
        if (!(shooter.level() instanceof ServerLevel level)) {
            return;
        }
        shooter.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            removeCardDisplay(level, data);
            removeCardModelDisplay(level, data);
            data.setCardTargetId(target.getId());
            data.setCardSum(0);
            data.setCardTicks(CARD_TICKS);
            data.setCardDisplayId(spawnCardDisplay(level, target, 0));
            // blackjack 카드 모델: 적에게서 시전자 쪽으로 떨어진 위치에 띄움
            data.setCardModelDisplayId(spawnItemDisplay(level,
                    cardModelPos(shooter, target), new ItemStack(ModItems.BLACKJACK.get())));
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
                clearCard(level, data);
            } else if (sum > BLACKJACK_TARGET) {
                clearCard(level, data);   // 버스트 — 꽝
            } else {
                data.setCardSum(sum);
                removeCardDisplay(level, data);
                data.setCardDisplayId(spawnCardDisplay(level, target, sum));
            }
        });
    }

    // 머리 위 "n / 21" 표시 생성
    private static int spawnCardDisplay(ServerLevel level, LivingEntity target, int sum) {
        Vec3 pos = new Vec3(target.getX(), target.getY() + target.getBbHeight() + 0.6, target.getZ());
        return spawnTextDisplay(level, pos,
                "{\"text\":\"" + sum + " / 21\",\"color\":\"gold\",\"bold\":true}");
    }

    private static void removeCardDisplay(ServerLevel level, PlayerCombatData data) {
        Entity display = level.getEntity(data.getCardDisplayId());
        if (display != null) {
            display.discard();
        }
        data.setCardDisplayId(-1);
    }

    // 카드 모델 위치: 적 가슴 높이에서, 시전자 방향으로 CARD_ORBIT_RADIUS만큼 떨어진 지점
    private static final double CARD_ORBIT_RADIUS = 0.9;

    private static Vec3 cardModelPos(LivingEntity shooter, Entity target) {
        Vec3 toShooter = new Vec3(shooter.getX() - target.getX(), 0, shooter.getZ() - target.getZ());
        toShooter = toShooter.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : toShooter.normalize();
        return new Vec3(
                target.getX() + toShooter.x * CARD_ORBIT_RADIUS,
                target.getY() + target.getBbHeight() * 0.55,
                target.getZ() + toShooter.z * CARD_ORBIT_RADIUS);
    }

    private static void removeCardModelDisplay(ServerLevel level, PlayerCombatData data) {
        Entity display = level.getEntity(data.getCardModelDisplayId());
        if (display != null) {
            display.discard();
        }
        data.setCardModelDisplayId(-1);
    }

    private static void clearCard(ServerLevel level, PlayerCombatData data) {
        removeCardDisplay(level, data);
        removeCardModelDisplay(level, data);
        data.setCardTargetId(-1);
        data.setCardSum(0);
        data.setCardTicks(0);
    }

    // 아이템 디스플레이 생성 (도적 단검 표시와 같은 NBT 방식 — setItemStack이 막혀 있어서)
    private static int spawnItemDisplay(ServerLevel level, Vec3 pos, ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:item_display");
        tag.put("item", stack.save(new CompoundTag()));
        tag.putString("billboard", "center");   // 항상 카메라를 향함
        Entity display = EntityType.loadEntityRecursive(tag, level, e -> {
            e.setPos(pos.x, pos.y, pos.z);
            return e;
        });
        if (display != null) {
            level.addFreshEntity(display);
            return display.getId();
        }
        return -1;
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
