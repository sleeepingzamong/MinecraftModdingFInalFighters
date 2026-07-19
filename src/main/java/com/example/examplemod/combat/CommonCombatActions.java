package com.example.examplemod.combat;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.entity.SkillProjectileEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class CommonCombatActions {

    public static void dash(ServerPlayer player, double dirX, double dirZ) {
    Vec3 direction;
    if (dirX == 0.0 && dirZ == 0.0) {
        Vec3 look = player.getLookAngle();          // WASD 없으면 바라보는 앞으로
        direction = new Vec3(look.x, 0, look.z);
    } else {
        direction = new Vec3(dirX, 0, dirZ);        // WASD 이동 방향으로
    }

    if (direction.lengthSqr() == 0) {
        return;
    }
    direction = direction.normalize();

    double dashPower = 1.0D;
    player.setDeltaMovement(direction.x * dashPower, 0.2D, direction.z * dashPower);
    player.hurtMarked = true;
}

    public static void fireProjectile(ServerPlayer player, double maxDistance) {
        fireProjectile(player, maxDistance, null, 6.0F, 1.0F);   // 기존(스킬1·2): 여의봉, 데미지 6, 히트박스 1배
    }

    public static void fireProjectile(ServerPlayer player, double maxDistance, ItemStack item, float damage) {
        fireProjectile(player, maxDistance, item, damage, 1.0F);
    }

    public static void fireProjectile(ServerPlayer player, double maxDistance, ItemStack item, float damage, float hitboxScale) {
        fireProjectile(player, maxDistance, item, damage, hitboxScale, 1.5F);   // 기본 속도
    }

    public static void fireProjectile(ServerPlayer player, double maxDistance, ItemStack item, float damage, float hitboxScale, float speed) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        SkillProjectileEntity projectile = new SkillProjectileEntity(level, player);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, 1.0F);
        projectile.setLaunchDirection(player.getYRot(), player.getXRot());
        projectile.setMaxDistance(maxDistance);
        if (item != null) {
            projectile.setItem(item);   // budda 등 모델 교체
        }
        // 복사 스킬 시전 중이면 데미지 +10%, 그리고 투사체에 "출처" 도장 찍기
        float finalDamage = damage;
        var dataOpt = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA);
        if (dataOpt.map(PlayerCombatData::isCopyBuffActive).orElse(false)) {
            finalDamage *= 1.1F;
        }
        projectile.setDamage(finalDamage);
        dataOpt.ifPresent(d -> projectile.setSource(d.getCastCharacter(), d.getCastSlot()));
        projectile.setHitboxScale(hitboxScale);
        level.addFreshEntity(projectile);
    }
}