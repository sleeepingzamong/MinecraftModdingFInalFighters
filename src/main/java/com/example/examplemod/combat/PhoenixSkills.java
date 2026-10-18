package com.example.examplemod.combat;

import com.example.examplemod.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// 불사조 스킬
// - Z 깃털: 원거리 투사체 (데미지 4 + 화상 2), 총알보다 느린 탄속, 쿨 0.75초
// - X 비상: 시선 방향 돌진(하늘 포함) + 회복 파동(반경 5 원 안 본인·아군 힐 2 + 보호막 2) + 느린낙하.
//           시전 지점에 반경 5칸 불꽃 원이 퍼져 범위를 그대로 보여줌.
//           쿨타임(4초)은 착지 후부터 (PhoenixEvents가 착지 감지)
// - 궁 재생의 불꽃: 발밑(공중이면 바로 아래 땅)에 반경 7칸 장판 8초 — 안의 본인·아군 초당 1 회복,
//                   적은 화상이 계속 갱신 (장판·불기둥·바닥 문양 처리는 PhoenixEvents)
public class PhoenixSkills implements CharacterSkills {

    private static final float FEATHER_DAMAGE = 4.0F;
    private static final float FEATHER_SPEED = 1.5F;      // 리볼버 총알(3.0)보다 느리게
    private static final double DASH_SPEED = 1.35D;
    private static final double WAVE_RADIUS = 5.0D;      // 회복 파동 반경 (원형 판정 — 불꽃 원과 일치)
    private static final float WAVE_HEAL = 2.0F;
    private static final float WAVE_SHIELD = 2.0F;        // 보호막 (중첩 없음 — 최대 2 유지)
    private static final int SLOW_FALL_TICKS = 100;       // 느린낙하 5초

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick) {
        // Z: 빨간 깃털 발사 — 명중 시 화상은 SkillProjectileEntity가 적용
        // 발사음은 커스텀 슬롯(phoenix_z.ogg)이 담당 — 바닐라 소리 없음
        CommonCombatActions.fireProjectile(player, 25,
                new ItemStack(ModItems.PHOENIX_FEATHER.get()), FEATHER_DAMAGE, 1.0F, FEATHER_SPEED);
    }

    @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // 1) 시선 방향 돌진 (수직 포함 — 하늘로도 날아오를 수 있음)
        Vec3 look = player.getLookAngle();
        Vec3 dir = look.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : look.normalize();
        player.setDeltaMovement(dir.scale(DASH_SPEED));
        player.hurtMarked = true;
        PhoenixEvents.startDash(player);   // 착지해야 쿨타임 시작

        // 2) 회복 파동: 반경 5 원 안 본인+아군 힐 2 + 보호막 2 (보호막은 중첩 없이 2 유지)
        //    박스가 아니라 실제 원(구) 판정 — 불꽃 원 밖 대각선 구석은 효과 없음
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(WAVE_RADIUS),
                e -> (e == player || e.isAlliedTo(player))
                        && e.distanceToSqr(player) <= WAVE_RADIUS * WAVE_RADIUS)) {
            ally.heal(WAVE_HEAL);
            ally.setAbsorptionAmount(Math.max(ally.getAbsorptionAmount(), WAVE_SHIELD));
            level.sendParticles(ParticleTypes.HEART,
                    ally.getX(), ally.getY() + ally.getBbHeight() + 0.3, ally.getZ(),
                    3, 0.3, 0.2, 0.3, 0.0);
        }

        // 3) 느린낙하 (돌진 직후)
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                SLOW_FALL_TICKS, 0, false, false, true));

        // 파동 연출: 효과 범위(반경 5)를 그대로 보여주는 바깥 불꽃 원 + 안쪽 보조 고리
        double baseY = player.getY() + 0.15;
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48.0;
            level.sendParticles(ParticleTypes.FLAME,
                    player.getX() + Math.cos(angle) * WAVE_RADIUS,
                    baseY,
                    player.getZ() + Math.sin(angle) * WAVE_RADIUS,
                    2, 0.08, 0.15, 0.08, 0.01);
        }
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24.0 + 0.13;
            level.sendParticles(ParticleTypes.FLAME,
                    player.getX() + Math.cos(angle) * (WAVE_RADIUS * 0.55),
                    baseY + 0.25,
                    player.getZ() + Math.sin(angle) * (WAVE_RADIUS * 0.55),
                    1, 0.08, 0.2, 0.08, 0.015);
        }
        // 비상음은 커스텀 슬롯(phoenix_x.ogg)이 담당 — 바닐라 소리 없음
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        PhoenixEvents.startRebirthZone(player);
        // 시전음은 궁 슬롯(phoenix_ult.ogg), 장판 지속음은 클라 PhoenixZoneSound가 담당 — 바닐라 소리 없음
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("재생의 불꽃 — 8초간 장판 안 아군 회복, 적은 화상"), true);
    }
}
