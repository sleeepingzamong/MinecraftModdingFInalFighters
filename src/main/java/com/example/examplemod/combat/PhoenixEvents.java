package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.PhoenixZoneS2CPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 불사조 처리:
// - 화상: 무기(깃털 부채) 평타·깃털(Z)·궁 장판이 거는 2초 도트 (초당 1, 재적중 시 지속시간만 갱신 — 중첩 없음)
// - X 돌진: 착지해야 쿨타임(4초)이 돌기 시작 (공중에서는 재사용 불가)
// - 궁 재생의 불꽃: 바닥에 반경 7칸 장판 8초 — 안의 본인·아군 초당 1 회복, 적은 화상이 계속 갱신
//                   (바닥 불사조 문양은 클라가 그림: PhoenixZoneS2CPacket → ClientPhoenixZones)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class PhoenixEvents {

    // ==================== 튜닝 상수 ====================
    private static final int BURN_TICKS = 40;            // 화상 지속 2초 (초당 1 → 총 2, 데미지 2틱 후 소멸)
    private static final double GLIDE_MAX_FALL = 0.04;   // X 활공 최대 하강 속도 (블록/틱) — 초당 0.8블록
    public static final int DASH_COOLDOWN_TICKS = 80;    // X 쿨 4초 (착지 시점부터)
    private static final int DASH_TIMEOUT_TICKS = 300;   // 착지 못 해도 15초 뒤엔 강제 쿨 시작
    private static final double ZONE_RADIUS = 7.0;       // 궁 장판 반경 (불기둥·불꽃 원 등 연출은 ClientPhoenixZones)
    private static final int ZONE_TICKS = 160;           // 궁 장판 지속 8초
    private static final float ZONE_HEAL = 1.0F;         // 1초마다 아군 회복량
    private static final double ZONE_BELOW = 1.0;        // 판정 높이: 장판 바닥 기준 아래 1칸 ~
    private static final double ZONE_ABOVE = 4.0;        //            위 4칸 (X로 높이 날면 범위 밖)
    // =================================================

    private static class BurnData {
        int ticksLeft = BURN_TICKS;   // 남은 지속시간
        int age = 0;                  // 걸린 뒤 경과 틱 — 1초마다 데미지 (갱신돼도 박자 유지)
        UUID attacker;
        boolean fromUltimate;         // 궁 장판이 건 화상 → 시전자 게이지 충전 없음

        BurnData(UUID attacker) {
            this.attacker = attacker;
        }
    }

    private static class Zone {
        final int id;                 // 클라 문양과 짝을 맞추는 번호
        final UUID caster;
        final ResourceKey<Level> dimension;
        final double x, y, z;         // 장판 중심 (y = 바닥 높이)
        int age = 0;

        Zone(int id, UUID caster, ResourceKey<Level> dimension, double x, double y, double z) {
            this.id = id;
            this.caster = caster;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final Map<UUID, BurnData> BURNING = new HashMap<>();      // 화상 걸린 대상
    private static final Map<UUID, Integer> DASH_AIRBORNE = new HashMap<>(); // X 사용 후 미착지 (값 = 경과 틱)
    private static final List<Zone> ZONES = new ArrayList<>();               // 펼쳐진 궁 장판
    private static int nextZoneId = 1;

    private static boolean dotDamage = false;   // 화상 도트 데미지 처리 중 플래그 (도트가 스스로 화상을 갱신하는 것 방지)

    // ===== 화상 =====

    public static void applyBurn(ServerPlayer attacker, LivingEntity target) {
        applyBurn(attacker, target, false);
    }

    // 재적중 = 지속시간 갱신 (중첩 없음). 장판은 0.5초마다 갱신하므로 머무는 동안 계속 탄다
    public static void applyBurn(ServerPlayer attacker, LivingEntity target, boolean fromUltimate) {
        if (target.isAlliedTo(attacker) || !target.isAlive()) {
            return;
        }
        BurnData burn = BURNING.get(target.getUUID());
        boolean fresh = burn == null;
        if (fresh) {
            burn = new BurnData(attacker.getUUID());
            BURNING.put(target.getUUID(), burn);
        }
        burn.ticksLeft = BURN_TICKS;
        burn.attacker = attacker.getUUID();
        burn.fromUltimate = fromUltimate;
        // 오른쪽 위 상태 아이콘 (표시 전용 — 남은 시간이 화상 지속시간과 같이 갱신됨)
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.example.examplemod.registry.ModEffects.BURN.get(), BURN_TICKS, 0, false, false, true));
        // 불붙는 순간 연출 — 장판의 반복 갱신 때는 생략 (파티클 폭주 방지)
        if ((fresh || !fromUltimate) && target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    6, 0.25, 0.3, 0.25, 0.02);
        }
    }

    // 무기(깃털 부채) 평타 명중 → 화상 (직접 근접 공격만)
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (dotDamage) {
            return;   // 화상 도트 데미지도 playerAttack이라 여기로 들어옴 → 스스로 화상 갱신 = 안 꺼지는 버그
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || event.getSource().getDirectEntity() != attacker) {
            return;
        }
        if (attacker.getMainHandItem().getItem() instanceof com.example.examplemod.item.FireBladeItem) {
            applyBurn(attacker, event.getEntity());
        }
    }

    // 궁 장판 + 화상 카운트다운 (월드 틱 — 몹·플레이어 공용)
    // 주의: hurt()·heal()이 사망·게이지 등 온갖 이벤트를 연쇄시키므로, 그 핸들러가 목록을
    // 건드려도(clear 등) 크래시가 없도록 원본이 아닌 스냅샷 목록을 순회한다.
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!ZONES.isEmpty()) {
            tickZones(level);
        }
        if (BURNING.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, BurnData> entry : new ArrayList<>(BURNING.entrySet())) {
            Entity e = level.getEntity(entry.getKey());
            if (e == null) {
                continue;   // 다른 차원/언로드 — 다음 틱에 다시 (같은 레벨에서만 처리)
            }
            if (!(e instanceof LivingEntity living) || !living.isAlive()) {
                BURNING.remove(entry.getKey());
                continue;
            }
            BurnData burn = entry.getValue();
            burn.age++;
            burn.ticksLeft--;
            if (burn.age % 5 == 0) {
                level.sendParticles(ParticleTypes.FLAME,
                        living.getX(), living.getY() + living.getBbHeight() * 0.6, living.getZ(),
                        2, 0.2, 0.25, 0.2, 0.01);
            }
            if (burn.age % 20 == 0) {
                // 초당 1 데미지 (공격자 크레딧 유지 — 킬 판정 연결. 게이지는 1만 충전, 궁 장판 화상은 시전자 0)
                ServerPlayer attacker = level.getServer().getPlayerList().getPlayer(burn.attacker);
                UltimateGaugeEvents.setBurnDamage(true);
                if (burn.fromUltimate) {
                    UltimateGaugeEvents.setUltimateDamage(true);
                }
                dotDamage = true;
                living.hurt(attacker != null
                        ? level.damageSources().playerAttack(attacker)
                        : level.damageSources().onFire(), 1.0F);
                dotDamage = false;
                if (burn.fromUltimate) {
                    UltimateGaugeEvents.setUltimateDamage(false);
                }
                UltimateGaugeEvents.setBurnDamage(false);
            }
            if (burn.ticksLeft <= 0) {
                BURNING.remove(entry.getKey());
                living.removeEffect(com.example.examplemod.registry.ModEffects.BURN.get());
            }
        }
    }

    // ===== 궁: 재생의 불꽃 장판 =====

    public static void startRebirthZone(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        // 공중(X 비상 중)에서 써도 장판은 바로 아래 땅에 깔림 (최대 12칸 아래까지)
        Vec3 from = caster.position().add(0, 0.5, 0);
        BlockHitResult hit = level.clip(new ClipContext(from, from.add(0, -12.5, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double groundY = hit.getType() == HitResult.Type.MISS ? caster.getY() : hit.getLocation().y;
        Zone zone = new Zone(nextZoneId++, caster.getUUID(), level.dimension(), caster.getX(), groundY, caster.getZ());
        ZONES.add(zone);
        // 바닥 문양·불기둥·불꽃 원은 이 패킷 하나로 차원 안 모든 클라가 직접 그림
        // (불사조 머리 = 시전자가 바라보던 방향)
        ModMessages.sendToDimension(new PhoenixZoneS2CPacket(zone.id, zone.x, zone.y, zone.z,
                (float) ZONE_RADIUS, ZONE_TICKS, caster.getYRot()), zone.dimension);
    }

    private static void endZone(Zone zone) {
        ZONES.remove(zone);
        ModMessages.sendToDimension(PhoenixZoneS2CPacket.remove(zone.id), zone.dimension);
    }

    private static void tickZones(ServerLevel level) {
        for (Zone zone : new ArrayList<>(ZONES)) {
            if (!zone.dimension.equals(level.dimension())) {
                continue;
            }
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(zone.caster);
            zone.age++;
            if (caster == null || zone.age > ZONE_TICKS) {
                endZone(zone);   // 시간 종료 또는 시전자 접속 종료
                continue;
            }
            boolean healTick = zone.age % 20 == 0;   // 1초마다 회복
            boolean burnTick = zone.age % 10 == 0;   // 0.5초마다 화상 갱신
            if (!healTick && !burnTick) {
                continue;
            }
            AABB box = new AABB(zone.x - ZONE_RADIUS, zone.y - ZONE_BELOW, zone.z - ZONE_RADIUS,
                    zone.x + ZONE_RADIUS, zone.y + ZONE_ABOVE, zone.z + ZONE_RADIUS);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    t -> t.isAlive() && !t.isSpectator() && !(t instanceof ArmorStand) && inZone(zone, t))) {
                if (target == caster || target.isAlliedTo(caster)) {
                    // 직접 회복이라 "맞은 직후 3.5초 자연회복 정지"와 무관하게 들어감
                    if (healTick && target.getHealth() < target.getMaxHealth()) {
                        target.heal(ZONE_HEAL);
                        level.sendParticles(ParticleTypes.HEART,
                                target.getX(), target.getY() + target.getBbHeight() + 0.3, target.getZ(),
                                1, 0.2, 0.1, 0.2, 0.0);
                    }
                } else if (burnTick) {
                    applyBurn(caster, target, true);
                }
            }
        }
    }

    private static boolean inZone(Zone zone, Entity e) {
        double dx = e.getX() - zone.x;
        double dz = e.getZ() - zone.z;
        return dx * dx + dz * dz <= ZONE_RADIUS * ZONE_RADIUS;
    }

    // ===== X 돌진 (착지 후 쿨) =====

    public static void startDash(ServerPlayer player) {
        DASH_AIRBORNE.put(player.getUUID(), 0);
    }

    // SkillManager 사전 체크 — 미착지 상태에서는 재사용 불가
    public static boolean isDashPending(ServerPlayer player) {
        return DASH_AIRBORNE.containsKey(player.getUUID());
    }

    // 활공 강화: 바닐라 느린낙하는 점점 가속돼(수 초 뒤 3~5블록/초) 하강 속도를 초당 0.8블록으로 캡
    // 이 모드에서 느린낙하 소스는 불사조 X뿐이라 플레이어 전체에 적용해도 안전.
    // 클라이언트에서도 돌아야 실제 낙하가 부드럽게 느려짐 (플레이어 이동은 클라 주도)
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)
                || !player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)
                || player.onGround() || player.isInWater() || player.isInLava()
                || player.isFallFlying() || player.getAbilities().flying) {
            return;
        }
        Vec3 v = player.getDeltaMovement();
        if (v.y < -GLIDE_MAX_FALL) {
            player.setDeltaMovement(v.x, -GLIDE_MAX_FALL, v.z);
        }
    }

    // PlayerTickHub가 매 틱 호출
    public static void tick(ServerPlayer player, ServerLevel level) {
        // X 착지 감지 (시전 0.5초 후부터 판정) → 쿨타임 시작 + 느린낙하 해제
        Integer air = DASH_AIRBORNE.get(player.getUUID());
        if (air != null) {
            int elapsed = air + 1;
            if ((elapsed > 10 && player.onGround()) || elapsed > DASH_TIMEOUT_TICKS || !player.isAlive()) {
                DASH_AIRBORNE.remove(player.getUUID());
                SkillManager.applyCooldown(player, SkillType.SKILL_2, DASH_COOLDOWN_TICKS);
                player.removeEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);   // 착지 → 느린낙하 끝
                if (player.isAlive() && player.onGround()) {
                    spawnLandingFlare(level, player);
                }
            } else {
                DASH_AIRBORNE.put(player.getUUID(), elapsed);
                // 비행 궤적: 불꽃 잔상
                level.sendParticles(ParticleTypes.FLAME,
                        player.getX(), player.getY() + 0.6, player.getZ(), 3, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    // X 착지 신호: 발밑에서 작은 불꽃 고리가 퍼짐 → "착지했다, 이제 쿨타임 시작"이 눈에 보이게
    private static void spawnLandingFlare(ServerLevel level, ServerPlayer player) {
        double x = player.getX(), y = player.getY() + 0.1, z = player.getZ();
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12.0;
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, Math.cos(a), 0.0, Math.sin(a), 0.1);
        }
        level.sendParticles(ParticleTypes.SMALL_FLAME, x, y + 0.2, z, 6, 0.3, 0.1, 0.3, 0.02);
    }

    // 상태초기화(/clearstatus) 시 정리 — 장판은 시전자가 죽어도 남은 시간 동안 유지 (접속 종료 시엔 자동 소멸)
    public static void clear(ServerPlayer player) {
        DASH_AIRBORNE.remove(player.getUUID());
        BURNING.remove(player.getUUID());   // 자기한테 걸린 화상도 해제
        player.removeEffect(com.example.examplemod.registry.ModEffects.BURN.get());
        for (Zone zone : new ArrayList<>(ZONES)) {
            if (zone.caster.equals(player.getUUID())) {
                endZone(zone);              // 펼친 궁 장판도 거둠 (클라 문양 포함)
            }
        }
    }
}
