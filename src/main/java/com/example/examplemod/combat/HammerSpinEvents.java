package com.example.examplemod.combat;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.item.HammerItem;
import com.example.examplemod.network.HammerSpinS2CPacket;
import com.example.examplemod.network.ModMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

// 해머 X — 회전 베기: 매초 주변 원 범위 데미지 + 스태미나 1 소모 (회복 정지)
// 실제 방향(yaw)은 안 건드림 — 회전은 클라이언트가 모델 렌더링만 돌려서 표현 (HammerSpinRenderEvents)
public class HammerSpinEvents {

    private static final double RADIUS = 3.5;               // 데미지 반경 (블록)
    private static final float DAMAGE_PER_SECOND = 3.0F;    // 초당 데미지
    private static final int STAMINA_PER_SECOND = 1;        // 초당 스태미나 소모
    private static final int STOP_COOLDOWN_TICKS = 20;      // 종료 후 재사용 쿨타임 (연타 토글 방지)

    // 회전 시작 (HammerSkills.useSkill2에서 호출 — 스태미나 1은 SkillManager가 이미 소모함)
    public static void startSpin(ServerPlayer player, PlayerCombatData data) {
        data.setHammerSpinning(true);
        data.setHammerSpinTimer(0);
        ModMessages.sendToTrackingAndSelf(new HammerSpinS2CPacket(player.getId(), true), player);
        if (player.level() instanceof ServerLevel level) {
            doSpinDamage(player, level);   // 시작 즉시 1회 타격
        }
    }

    public static void stopSpin(ServerPlayer player, PlayerCombatData data) {
        data.setHammerSpinning(false);
        data.setHammerSpinTimer(0);
        ModMessages.sendToTrackingAndSelf(new HammerSpinS2CPacket(player.getId(), false), player);
        // 종료 시점부터 1초 쿨타임
        data.setSkillReadyTick(SkillType.SKILL_2, player.level().getGameTime() + STOP_COOLDOWN_TICKS);
    }

    // X 재입력 토글: 회전 중이었으면 종료하고 true (SkillManager가 자원 소모 전에 호출)
    public static boolean toggleOff(ServerPlayer player) {
        return player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).map(data -> {
            if (!data.isHammerSpinning()) {
                return false;
            }
            stopSpin(player, data);
            return true;
        }).orElse(false);
    }

    public static boolean isSpinning(ServerPlayer player) {
        return player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(PlayerCombatData::isHammerSpinning)
                .orElse(false);
    }

    // PlayerTickHub가 매 틱 호출
    public static void tick(ServerPlayer player, ServerLevel level, PlayerCombatData data) {
        if (!data.isHammerSpinning()) {
            return;
        }

        // 망치를 놓으면 즉시 종료
        if (!(player.getMainHandItem().getItem() instanceof HammerItem)) {
            stopSpin(player, data);
            return;
        }

        int timer = data.getHammerSpinTimer() + 1;
        if (timer >= 20) {   // 1초마다
            timer = 0;

            // 스태미나 1 소모 — 부족하면 종료
            if (data.getStamina() < STAMINA_PER_SECOND) {
                stopSpin(player, data);
                return;
            }
            data.setStamina(data.getStamina() - STAMINA_PER_SECOND);
            CombatSync.syncStats(player);

            doSpinDamage(player, level);

            // 방금 소모로 0이 됐으면 이번 타격을 끝으로 종료
            if (data.getStamina() <= 0) {
                stopSpin(player, data);
                return;
            }
        }
        data.setHammerSpinTimer(timer);
    }

    private static void doSpinDamage(ServerPlayer player, ServerLevel level) {
        AABB box = player.getBoundingBox().inflate(RADIUS, 1.5, RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive())) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            if (dx * dx + dz * dz <= RADIUS * RADIUS) {
                target.hurt(level.damageSources().playerAttack(player), DAMAGE_PER_SECOND);
            }
        }

        // 휘두르는 소리 + 반경 가장자리에 칼바람 파티클 링
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.9F);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX() + Math.cos(angle) * 2.5,
                    player.getY() + 1.0,
                    player.getZ() + Math.sin(angle) * 2.5,
                    1, 0, 0, 0, 0);
        }
    }
}
