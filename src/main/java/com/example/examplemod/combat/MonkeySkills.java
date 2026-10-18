package com.example.examplemod.combat;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.registry.ModItems;
import com.example.examplemod.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MonkeySkills implements CharacterSkills {

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 direction = lookDirection(player);
        if (direction.lengthSqr() == 0) {
            return;
        }

        // 날아가는 여의봉은 순수 연출 (데미지 0) — 실제 타격은 아래 sweepAttack 하나만
        // (투사체가 기본 6뎀을 갖고 있어 비차징 시 둘 다 맞아 2번 들어가던 버그 수정)
        CommonCombatActions.fireProjectile(player, 4, null, 0.0F);

        double chargeRate = getChargeRate(chargeTick);
        double damage = 4.0D + chargeRate * 2.0D;
        double knockbackPower = 1.2D + chargeRate * 1.2D;

        sweepAttack(player, level, direction, 4, 1.0D, 2.5D, 2.5D,
                center -> level.sendParticles(ParticleTypes.CRIT,
                        center.x, center.y, center.z,
                        8 + (int) (chargeRate * 10), 0.3, 0.3, 0.3, 0.05),
                target -> {
                    target.hurt(player.damageSources().playerAttack(player), (float) damage);
                    Vec3 knockback = target.position().subtract(player.position()).normalize();
                    target.push(knockback.x * knockbackPower, 0.25 + chargeRate * 0.2, knockback.z * knockbackPower);
                });

        // player.sendSystemMessage(Component.literal("원숭이 스킬 1 - 밀치기! 차징: " + chargeTick + "틱"));
    }

    @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 direction = lookDirection(player);
        if (direction.lengthSqr() == 0) {
            return;
        }

        // 날아가는 여의봉은 순수 연출 (데미지 0) — 실제 타격은 아래 sweepAttack 하나만
        CommonCombatActions.fireProjectile(player, 8, null, 0.0F);

        double chargeRate = getChargeRate(chargeTick);
        double pullPower = 2.0D + chargeRate * 1.3D;
        double damage = 2.0D + chargeRate * 3.0D;

        sweepAttack(player, level, direction, 8, 1.0D, 2.5D, 2.5D,
                center -> level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        center.x, center.y, center.z,
                        2 + (int) (chargeRate * 4), 0.2, 0.2, 0.2, 0.01),
                target -> {
                    Vec3 pullDirection = player.position().subtract(target.position()).normalize();
                    target.push(pullDirection.x * pullPower, 0.2, pullDirection.z * pullPower);
                    target.hurt(player.damageSources().playerAttack(player), (float) damage);
                });

        // player.sendSystemMessage(Component.literal("원숭이 스킬 2 - 끌어오기! 차징: " + chargeTick + "틱"));
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // 여래신장: 효과음(모두에게) → 2초 충전(제자리 고정) → budda 발사 (발사는 MonkeyUltimateEvents가)
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // 접속한 모든 플레이어에게 각자 위치에서 재생 → 거리 무관하게 다 들림
        long seed = level.getRandom().nextLong();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSoundPacket(
                    Holder.direct(ModSounds.BUDDA.get()), SoundSource.PLAYERS,
                    p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F, seed));
        }

        float damage = 18.0F;   // 고정 (차징 제거)

        // 2초(40틱) 뒤 발사 예약
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            data.setBuddaWindupTicks(40);
            data.setBuddaDamage(damage);
        });
    }

    // ===== 공통 헬퍼 =====

    // 바라보는 방향 (위아래 포함한 3D 방향 그대로)
    private Vec3 lookDirection(ServerPlayer player) {
        return player.getLookAngle();
    }

    // 앞으로 range만큼 훑으며 적을 찾아 처리하는 공통 루프
    private void sweepAttack(
            ServerPlayer player,
            ServerLevel level,
            Vec3 direction,
            int range,
            double verticalOffset,
            double horizontalSize,
            double verticalSize,
            Consumer<Vec3> onCenter,
            Consumer<LivingEntity> onHit
    ) {
        Set<LivingEntity> hitEntities = new HashSet<>();

        for (int i = 1; i <= range; i++) {
            Vec3 center = player.position()
                    .add(0, verticalOffset, 0)
                    .add(direction.scale(i));

            AABB attackBox = AABB.ofSize(center, horizontalSize, verticalSize, horizontalSize);

            List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class,
                    attackBox,
                    entity -> entity.isAlive() && entity != player && !entity.isAlliedTo(player)
            );

            onCenter.accept(center);

            for (LivingEntity target : targets) {
                if (hitEntities.contains(target)) {
                    continue;
                }
                onHit.accept(target);
                hitEntities.add(target);
            }
        }
    }

    private double getChargeRate(int chargeTick) {
        int maxChargeTick = 40;
        int clampedChargeTick = Math.min(chargeTick, maxChargeTick);
        return clampedChargeTick / (double) maxChargeTick;
    }
}
