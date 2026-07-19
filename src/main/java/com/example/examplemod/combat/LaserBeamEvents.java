package com.example.examplemod.combat;

import com.example.examplemod.entity.LaserBeamEntity;
import com.example.examplemod.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// 사이보그 궁극기 레이저: 충전(2초) → 발사(3초). (PlayerTickHub가 매 틱 호출)
public class LaserBeamEvents {

    private static final double RANGE  = 30.0;   // 최대 사거리(칸)
    private static final float  DAMAGE = 4.0F;   // 적중당 데미지
    private static final double WIDTH  = 1.2;    // 빔 두께(적 판정 반경) — 보이는 굵기 2배에 맞춤

    // PlayerTickHub가 매 틱 호출 (이미 조회한 data를 받아 재조회 없음)
    public static void tick(ServerPlayer player, ServerLevel level, PlayerCombatData data) {
        int charge = data.getLaserChargeTicks();
        if (charge > 0) {
            // 충전 중: 발 묶기만, 레이저는 아직
            rootPlayer(player);
            data.setLaserChargeTicks(charge - 1);
            if (charge - 1 == 0) {
                // 충전 끝 → 빔 닻 엔티티 생성 (렌더러가 이걸 보고 통짜 빔을 그림)
                LaserBeamEntity beam = new LaserBeamEntity(ModEntities.LASER_BEAM.get(), level);
                beam.setPos(player.getX(), player.getY(), player.getZ());
                beam.setOwnerUUID(player.getUUID());
                level.addFreshEntity(beam);
                data.setLaserBeamEntityId(beam.getId());
            }
            return;
        }

        int left = data.getLaserTicksLeft();
        if (left > 0) {
            fireLaserTick(player);
            data.setLaserTicksLeft(left - 1);
            if (left - 1 <= 0) {
                // 발사 종료 → 빔 엔티티 제거
                Entity beam = level.getEntity(data.getLaserBeamEntityId());
                if (beam != null) {
                    beam.discard();
                }
                data.setLaserBeamEntityId(-1);
            }
        }
    }

    // 제자리 고정 (회전은 그대로 허용)
    private static void rootPlayer(ServerPlayer player) {
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(0.0, v.y, 0.0);
        player.hurtMarked = true;
    }

    private static void fireLaserTick(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        rootPlayer(player);

        // 눈 → 바라보는 방향 광선
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(RANGE));

        // 벽에서 멈춤
        BlockHitResult hit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 tip = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        double len = start.distanceTo(tip);

        // 광선 위 적에게 데미지
        AABB region = new AABB(start, tip).inflate(WIDTH);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, region,
                e -> e.isAlive() && e != player)) {
            Vec3 c = t.getBoundingBox().getCenter();
            double proj = c.subtract(start).dot(look);        // 광선 진행 방향 투영 길이
            if (proj < 0 || proj > len) {
                continue;                                      // 빔 구간 밖
            }
            Vec3 closest = start.add(look.scale(proj));        // 광선 위 가장 가까운 점
            if (closest.distanceTo(c) <= WIDTH + t.getBbWidth() / 2.0) {
                // 빔에 닿아 있는 동안 발광 (매 틱 갱신 → 벗어나면 곧 꺼짐)
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 15, 0, false, false));
                UltimateGaugeEvents.setUltimateDamage(true);   // 궁극기 데미지 → 시전자 게이지 충전 없음
                t.hurt(player.damageSources().playerAttack(player), DAMAGE);
                UltimateGaugeEvents.setUltimateDamage(false);
            }
        }

        // 빔 주변 빨간 파티클 (동적 연출 — 빔 본체는 LaserBeamRenderer가 모델로 그림)
        for (double d = 0.5; d < len; d += 1.5) {
            Vec3 p = start.add(look.scale(d));
            level.sendParticles(DustParticleOptions.REDSTONE,
                    p.x, p.y, p.z, 2, 0.4, 0.4, 0.5, 0.0);
        }
    }
}
