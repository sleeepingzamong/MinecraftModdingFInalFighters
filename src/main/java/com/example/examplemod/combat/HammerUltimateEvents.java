package com.example.examplemod.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 해머 궁 "대지 균열": 시전 0.5초 후(내려찍는 순간) 전방 부채꼴 바닥이 갈라지며
// 범위 안 적에게 데미지 + 스턴(3초, 공격당하면 깨어남)
public class HammerUltimateEvents {

    // ==================== 튜닝 상수 ====================
    private static final int IMPACT_DELAY_TICKS = 10;   // 시전 후 발동까지 (10틱 = 0.5초)
    private static final double RANGE = 14.0;           // 균열 길이 (블록)
    private static final double CONE_COS = 0.7071;      // 부채꼴 폭 (cos45° = 총 90도)
    private static final float DAMAGE = 8.0F;           // 균열 데미지
    private static final int STUN_TICKS = 60;           // 스턴 3초 (공격당하면 해제)
    // =================================================

    private static final int CAST_LOCK_TICKS = 30;      // 시전 경직 (시전 모션 1.5초와 동일)

    private static final Map<UUID, Long> PENDING = new HashMap<>();   // 시전자 → 균열 발동 예정 시각
    private static final Map<UUID, Long> CASTING = new HashMap<>();   // 시전자 → 시전 경직 종료 시각

    // HammerSkills.useUltimate가 호출
    public static void schedule(ServerPlayer player) {
        long now = player.level().getGameTime();
        PENDING.put(player.getUUID(), now + IMPACT_DELAY_TICKS);
        CASTING.put(player.getUUID(), now + CAST_LOCK_TICKS);
    }

    // 시전 경직 중인지 (SkillManager가 스킬·대쉬 차단에 사용)
    public static boolean isCasting(ServerPlayer player) {
        Long until = CASTING.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    public static void clear(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        CASTING.remove(player.getUUID());
    }

    // PlayerTickHub가 매 틱 호출
    public static void tick(ServerPlayer player, ServerLevel level) {
        // 시전 경직: 이동 거의 불가 (모션이 끝나면 자동 해제)
        Long castUntil = CASTING.get(player.getUUID());
        if (castUntil != null) {
            if (level.getGameTime() >= castUntil || !player.isAlive()) {
                CASTING.remove(player.getUUID());
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4, 6, false, false));
            }
        }

        Long impactAt = PENDING.get(player.getUUID());
        if (impactAt == null) {
            return;
        }
        if (!player.isAlive()) {
            PENDING.remove(player.getUUID());
            return;
        }
        if (level.getGameTime() < impactAt) {
            return;
        }
        PENDING.remove(player.getUUID());
        strike(player, level);
    }

    // 균열 발동: 발동 "순간"의 시선 방향 기준 부채꼴
    private static void strike(ServerPlayer player, ServerLevel level) {
        Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : look.normalize();

        // 충격음: 낮은 폭발 + 모루
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.6F);

        // 균열 파티클: 부챗살 9줄 × 0.5블록 간격 — 그 지점 바닥 블록의 파편이 튐
        double baseYaw = Math.atan2(look.z, look.x);
        for (int ray = 0; ray <= 8; ray++) {
            double angle = baseYaw + Math.toRadians(-45 + ray * 11.25);
            double dirX = Math.cos(angle), dirZ = Math.sin(angle);
            for (double dist = 1.0; dist <= RANGE; dist += 0.5) {
                double px = player.getX() + dirX * dist;
                double pz = player.getZ() + dirZ * dist;
                BlockPos below = BlockPos.containing(px, player.getY() - 0.5, pz);
                BlockState state = level.getBlockState(below);
                if (state.isAir()) {
                    continue;   // 허공(절벽 밖)은 균열 없음
                }
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        px, player.getY() + 0.1, pz, 4, 0.15, 0.08, 0.15, 0.1);
            }
        }

        // 판정: 전방 부채꼴 안의 모든 생명체 — 데미지 먼저, 스턴은 그다음 (순서 중요:
        // 스턴을 먼저 걸면 균열 데미지 자체가 "공격당함"으로 집계돼 즉시 깨어남)
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE, 2.0, RANGE))) {
            if (target == player || !target.isAlive() || target.isAlliedTo(player)) {
                continue;
            }
            Vec3 to = target.position().subtract(player.position());
            Vec3 to2d = new Vec3(to.x, 0, to.z);
            if (to2d.length() > RANGE) {
                continue;
            }
            if (to2d.length() > 0.5D && to2d.normalize().dot(look) < CONE_COS) {
                continue;   // 부채꼴 밖
            }

            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), DAMAGE);
            StunManager.stun(target, STUN_TICKS, true);   // 공격당하면 깨어나는 스턴
        }
    }
}
