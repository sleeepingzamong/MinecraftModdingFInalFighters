package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 눈덩이에 맞은 플레이어는 스턴 (범용 StunManager 사용)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class SnowballStunEvents {

    private static final int STUN_TICKS = 40;    // 스턴 2초 (해머 투척과 동일)
    private static final int FREEZE_TICKS = 139; // 빙결 게이지 (140이면 동상 데미지가 시작되므로 그 직전까지)
                                                 // 가루눈에 들어간 것처럼 화면에 얼음 결정이 가득 → 서서히 녹음

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball snowball)) {
            return;
        }
        if (!(snowball.level() instanceof ServerLevel level)) {
            return;   // 서버에서만 판정
        }
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) {
            return;   // 블록에 맞은 건 무시
        }
        if (!(entityHit.getEntity() instanceof ServerPlayer target)) {
            return;   // 플레이어만 스턴
        }
        if (snowball.getOwner() != null && target.isAlliedTo(snowball.getOwner())) {
            return;   // 같은 팀 눈덩이는 스턴 없음
        }

        StunManager.stun(target, STUN_TICKS);

        // 가루눈 빙결 연출: 맞은 플레이어 화면에 얼음 결정 오버레이 (틱마다 2씩 자연 감소하며 녹음)
        if (target.getTicksFrozen() < FREEZE_TICKS) {
            target.setTicksFrozen(FREEZE_TICKS);
        }

        // 명중 연출: 눈 부서지는 소리 + 눈송이 파티클
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 1.0F, 1.3F);
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + 1.2D, target.getZ(), 15, 0.3D, 0.4D, 0.3D, 0.05D);
    }
}
