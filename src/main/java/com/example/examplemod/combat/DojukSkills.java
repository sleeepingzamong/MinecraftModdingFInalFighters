package com.example.examplemod.combat;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DojukSkills implements CharacterSkills {

    // Z 순간이동 대상 찾기 (시선 11칸 내 가장 가까운 생물) — 사전 체크와 시전이 공유
    public static LivingEntity findTeleportTarget(ServerPlayer player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = 11.0;
        AABB box = new AABB(eye, eye.add(look.scale(range))).inflate(1.0);

        LivingEntity target = null;
        double bestProj = range;
        for (LivingEntity p : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player))) {   // 같은 팀 제외
            Vec3 c = p.getBoundingBox().getCenter();
            double proj = c.subtract(eye).dot(look);          // 시선 방향 거리
            if (proj < 0 || proj > range) {
                continue;                                      // 뒤/너무 멀면 제외
            }
            if (eye.add(look.scale(proj)).distanceTo(c) <= 1.0 && proj < bestProj) {
                bestProj = proj;
                target = p;                                    // 시선에 닿는 가장 가까운 생물
            }
        }
        return target;
    }

    // SkillManager가 자원 소모 전에 확인 — 대상 없으면 스태미나·쿨타임을 아예 안 씀
    public static boolean hasTeleportTarget(ServerPlayer player) {
        return player.level() instanceof ServerLevel level && findTeleportTarget(player, level) != null;
    }

    // 표식 유지 중이거나 돌진 중인지 — 이때의 X 재입력(돌진 시작·취소)은 표창 쿨타임을 무시해야 함
    public static boolean isMarkOrDashActive(ServerPlayer player) {
        return player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(d -> (d.getDojukMarkTicks() > 0 && d.getDojukMarkTargetId() != -1)
                        || d.getDojukDashTicks() > 0)
                .orElse(false);
    }

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick) {
        // Z 스킬: 바라보는 생물(플레이어·몹)의 "등 뒤"로 순간이동
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target = findTeleportTarget(player, level);
        if (target == null) {
            return;   // SkillManager가 미리 막지만 만약을 위해
        }

        // 2) 대상의 바라보는 방향 기준 "등 뒤" 1칸 위치
        double yawRad = Math.toRadians(target.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));   // 대상 정면(수평)
        Vec3 tp = target.position().subtract(forward.scale(1.0));          // 정면 반대 = 등 뒤

        // 3) 순간이동 + 대상과 같은 방향 보기(= 대상 등을 바라봄)
        player.teleportTo(level, tp.x, tp.y, tp.z, target.getYRot(), 0.0F);
    }

    @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        // X 스킬: 표창 → (명중 시) 돌진 → (돌진 중 재입력) 취소
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // 돌진 중이면 → 취소하고 멈춤
        boolean dashing = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(d -> d.getDojukDashTicks() > 0)
                .orElse(false);
        if (dashing) {
            player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
                data.setDojukDashTicks(0);
                data.setDojukMarkTargetId(-1);
            });
            // 관성 죽이기 (안 하면 날던 속도로 미끄러짐)
            player.setDeltaMovement(player.getDeltaMovement().scale(0.2));
            player.hurtMarked = true;
            StaminaManager.recoverStamina(player, 1);   // 취소는 스태미나 안 씀(환불)
            return;
        }

        boolean hasMark = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(d -> d.getDojukMarkTicks() > 0 && d.getDojukMarkTargetId() != -1)
                .orElse(false);

        if (hasMark) {
            // 돌진 시작: 표식 소모(카운트다운 중지 + 머리 위 단검 제거), 대상 ID는 돌진 추적용으로 유지
            player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
                Entity display = level.getEntity(data.getDojukMarkDisplayId());
                if (display != null) {
                    display.discard();
                }
                // 표식 소모 → 시전자 화면의 발광도 끔
                com.example.examplemod.network.ModMessages.sendToPlayer(
                        new com.example.examplemod.network.MarkGlowS2CPacket(data.getDojukMarkTargetId(), 0), player);
                data.setDojukMarkDisplayId(-1);
                data.setDojukMarkTicks(0);
                data.setDojukDashTicks(DojukMarkEvents.DASH_TICKS);
            });
            StaminaManager.recoverStamina(player, 1);   // 돌진은 스태미나 추가 소모 없음(환불)
        } else {
            // 표창: knife 모델 투사체 (데미지 2, 사거리 15)
            CommonCombatActions.fireProjectile(player, 15, new ItemStack(ModItems.KNIFE.get()), 2.0F);

            // 표창에만 1초 쿨타임 (CharacterStats는 0으로 두고 여기서 직접 —
            // 돌진 시작·취소 재입력이 쿨에 막히지 않게 하기 위함)
            final long readyAt = level.getGameTime() + 20L;
            player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                    .ifPresent(data -> data.setSkillReadyTick(SkillType.SKILL_2, readyAt));
            com.example.examplemod.network.ModMessages.sendToPlayer(
                    new com.example.examplemod.network.CooldownS2CPacket(SkillType.SKILL_2, 20), player);
        }
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // 궁극기: 15초 완전 투명화 (2초마다 잔상, 투명 중 공격 시 추가 데미지 후 해제. 아이콘 표시)
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                DojukMarkEvents.INVIS_TICKS, 0, false, false, true));
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data ->
                data.setDojukInvisTicks(DojukMarkEvents.INVIS_TICKS));
    }
}
