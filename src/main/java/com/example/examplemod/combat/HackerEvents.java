package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.SystemHackS2CPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 해커 스킬 처리 (컨셉: 시스템 교란)
// - Z 롤백: 3초 전 내 위치로 순간이동, 떠난 자리에 글리치 잔상 → 잠깐 뒤 터지며 주변 적 스턴
// - X 버그 무시: 0.5초 동안 모든 공격 무효
// - 궁 시스템 해킹: 바라보는 플레이어의 이동키 반전 + 화면 글리치 (클라이언트 HackerClientEvents가 연출)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class HackerEvents {

    // ==================== 튜닝 상수 ====================
    private static final int ROLLBACK_HISTORY_TICKS = 60;   // 몇 틱 전 위치로 돌아갈지 (60틱 = 3초)
    private static final int GLITCH_DELAY_TICKS = 10;       // 잔상이 터지기까지 (0.5초)
    private static final double GLITCH_RADIUS = 2.5D;       // 잔상 폭발 스턴 범위
    private static final int GLITCH_STUN_TICKS = 30;        // 잔상 스턴 1.5초

    private static final int BUG_IGNORE_TICKS = 40;         // 버그 무시 2초 (무적 + 스턴·둔화 면역)

    private static final double HACK_RANGE = 12.0D;         // 궁 대상 지정 사거리
    private static final int HACK_TICKS = 400;              // 시스템 해킹 지속 20초
    // =================================================

    private static final Map<UUID, Deque<Vec3>> HISTORY = new HashMap<>();        // 최근 3초 위치 기록
    private static final Map<UUID, Long> BUG_IGNORE_UNTIL = new HashMap<>();      // 버그 무시 종료 시각
    private static final Map<UUID, Long> HACKED_UNTIL = new HashMap<>();          // 해킹당한 플레이어 (사망 시 조기 해제용)
    private static final List<GlitchBomb> PENDING_GLITCH = new ArrayList<>();     // 터지기를 기다리는 잔상

    private record GlitchBomb(UUID owner, Vec3 pos, long burstTick) {}

    // ===== Z 롤백 =====
    public static void rollback(ServerPlayer player, ServerLevel level) {
        Deque<Vec3> history = HISTORY.get(player.getUUID());
        if (history == null || history.isEmpty()) {
            player.sendSystemMessage(Component.literal("위치 기록이 없습니다."));
            StaminaManager.recoverStamina(player, 1);   // 헛방이면 환불
            return;
        }

        Vec3 past = history.peekFirst();   // 가장 오래된 기록 (≈3초 전)
        Vec3 departure = player.position();

        // 떠난 자리에 글리치 잔상 예약 (0.5초 뒤 폭발 — 쫓아오던 적이 밟는 그림)
        long now = level.getGameTime();
        PENDING_GLITCH.add(new GlitchBomb(player.getUUID(), departure, now + GLITCH_DELAY_TICKS));

        // 연출: 출발지·도착지 양쪽에 포탈+스파크
        level.playSound(null, departure.x, departure.y, departure.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, departure.x, departure.y + 1.0D, departure.z,
                20, 0.3D, 0.7D, 0.3D, 0.05D);

        player.teleportTo(level, past.x, past.y, past.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;

        level.playSound(null, past.x, past.y, past.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, past.x, past.y + 1.0D, past.z,
                15, 0.3D, 0.7D, 0.3D, 0.2D);

        // 기록 초기화 — 연속 롤백으로 과거의 과거를 오가는 왕복 방지
        history.clear();
    }

    // ===== X 버그 무시 =====
    public static void bugIgnore(ServerPlayer player, ServerLevel level) {
        BUG_IGNORE_UNTIL.put(player.getUUID(), level.getGameTime() + BUG_IGNORE_TICKS);
        // 바이러스 2마리 공전 연출 (모두에게 보임)
        ModMessages.sendToTrackingAndSelf(
                new com.example.examplemod.network.BugIgnoreS2CPacket(player.getId(), BUG_IGNORE_TICKS), player);
        player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);   // 걸려있던 둔화도 제거
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.8F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.3D, 0.6D, 0.3D, 0.15D);
        player.displayClientMessage(Component.literal("버그 무시 — 2초 무적·CC 면역"), true);
    }

    // 버그 무시 중인지 (StunManager가 스턴 차단에 사용)
    public static boolean isBugIgnoring(ServerPlayer player) {
        Long until = BUG_IGNORE_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    // 버그 무시 중엔 둔화 효과도 안 먹음 (스턴 슬로우·기타 둔화 전부)
    @SubscribeEvent
    public static void onEffectApplicable(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isBugIgnoring(player)) {
            return;
        }
        if (event.getEffectInstance().getEffect() == net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    // ===== 궁 시스템 해킹 =====
    public static void systemHack(ServerPlayer player, ServerLevel level) {
        ServerPlayer target = findLookPlayer(player, level, HACK_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("대상(플레이어)이 없습니다."));
            UltimateGaugeManager.addGauge(player, 1);   // 헛방이면 게이지 환불
            return;
        }

        HACKED_UNTIL.put(target.getUUID(), level.getGameTime() + HACK_TICKS);
        ModMessages.sendToPlayer(new SystemHackS2CPacket(HACK_TICKS), target);

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.5F, 1.5F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + 1.2D, target.getZ(), 25, 0.4D, 0.6D, 0.4D, 0.3D);
        player.displayClientMessage(Component.literal(
                "시스템 해킹 성공! (" + (HACK_TICKS / 20) + "초)"), true);
    }

    // 버그 무시: 데미지 계산 전에 공격 자체를 무효화
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        Long until = BUG_IGNORE_UNTIL.get(victim.getUUID());
        if (until == null || victim.level().getGameTime() >= until) {
            return;
        }
        event.setCanceled(true);
        if (victim.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    victim.getX(), victim.getY() + 1.0D, victim.getZ(), 5, 0.3D, 0.4D, 0.3D, 0.2D);
        }
    }

    // 해킹당한 채로 죽으면 교란 조기 해제
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (HACKED_UNTIL.remove(player.getUUID()) != null) {
            ModMessages.sendToPlayer(new SystemHackS2CPacket(0), player);
        }
        HISTORY.remove(player.getUUID());   // 죽은 위치로 롤백 못 하게 기록 초기화
    }

    // PlayerTickHub가 매 틱 호출
    public static void tick(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();

        // 1) 위치 기록 (모든 플레이어 — 해커가 아니어도 부담 없는 수준)
        Deque<Vec3> history = HISTORY.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
        history.addLast(player.position());
        while (history.size() > ROLLBACK_HISTORY_TICKS + 1) {
            history.removeFirst();
        }

        // 2) 글리치 잔상: 대기 중엔 지지직 파티클, 시간이 되면 폭발 + 주변 적 스턴
        if (!PENDING_GLITCH.isEmpty()) {
            Iterator<GlitchBomb> it = PENDING_GLITCH.iterator();
            while (it.hasNext()) {
                GlitchBomb bomb = it.next();
                if (!bomb.owner().equals(player.getUUID())) {
                    continue;
                }
                if (now < bomb.burstTick()) {
                    if (now % 2 == 0) {
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                bomb.pos().x, bomb.pos().y + 1.0D, bomb.pos().z, 4, 0.3D, 0.6D, 0.3D, 0.1D);
                    }
                    continue;
                }
                it.remove();
                level.playSound(null, bomb.pos().x, bomb.pos().y, bomb.pos().z,
                        SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2F, 0.8F);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        bomb.pos().x, bomb.pos().y + 1.0D, bomb.pos().z, 30, 0.6D, 0.8D, 0.6D, 0.4D);
                AABB area = new AABB(bomb.pos(), bomb.pos()).inflate(GLITCH_RADIUS, 1.5D, GLITCH_RADIUS);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area,
                        e -> e.isAlive() && !e.getUUID().equals(bomb.owner()))) {
                    StunManager.stun(victim, GLITCH_STUN_TICKS);
                }
            }
        }

        // 3) 해킹 지속시간 만료 정리 (클라는 자체 카운트다운으로 스스로 풀림)
        Long hackedUntil = HACKED_UNTIL.get(player.getUUID());
        if (hackedUntil != null && now >= hackedUntil) {
            HACKED_UNTIL.remove(player.getUUID());
        }
    }

    // 로그아웃·리스폰 시 상태 정리
    public static void clear(ServerPlayer player) {
        HISTORY.remove(player.getUUID());
        BUG_IGNORE_UNTIL.remove(player.getUUID());
        PENDING_GLITCH.removeIf(bomb -> bomb.owner().equals(player.getUUID()));
        if (HACKED_UNTIL.remove(player.getUUID()) != null) {
            ModMessages.sendToPlayer(new SystemHackS2CPacket(0), player);
        }
    }

    // 시선 방향의 가장 가까운 "플레이어" (몹은 교란 대상이 아님)
    private static ServerPlayer findLookPlayer(ServerPlayer player, ServerLevel level, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB box = new AABB(eye, eye.add(look.scale(range))).inflate(1.0D);

        ServerPlayer target = null;
        double bestProj = range;
        for (ServerPlayer e : level.getEntitiesOfClass(ServerPlayer.class, box,
                x -> x != player && x.isAlive())) {
            Vec3 c = e.getBoundingBox().getCenter();
            double proj = c.subtract(eye).dot(look);
            if (proj < 0 || proj > range) {
                continue;
            }
            if (eye.add(look.scale(proj)).distanceTo(c) <= 1.0D && proj < bestProj) {
                bestProj = proj;
                target = e;
            }
        }
        return target;
    }
}
