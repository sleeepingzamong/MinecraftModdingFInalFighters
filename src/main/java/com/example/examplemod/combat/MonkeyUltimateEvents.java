package com.example.examplemod.combat;

import com.example.examplemod.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// 원숭이 궁극기(여래신장): 2초 충전(제자리 고정) 후 budda 발사. (PlayerTickHub가 매 틱 호출)
public class MonkeyUltimateEvents {

    public static void tick(ServerPlayer player, PlayerCombatData data) {
        int windup = data.getBuddaWindupTicks();
        if (windup <= 0) {
            return;
        }

        // 충전 중: 제자리 고정 (회전만 가능)
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(0.0, v.y, 0.0);
        player.hurtMarked = true;

        if (windup == 1) {
            // 충전 끝 → 그 순간 바라보는 방향으로 발사
            CommonCombatActions.fireProjectile(player, 40,
                    new ItemStack(ModItems.BUDDA.get()), data.getBuddaDamage(), 5.0F);
        }
        data.setBuddaWindupTicks(windup - 1);
    }
}
