package com.example.examplemod.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

// 배고픔 끄기: 매 틱 포만도를 가득 유지해서 절대 줄지 않게 한다. (PlayerTickHub가 호출)
public class NoHungerEvents {

    public static void tick(ServerPlayer player) {
        FoodData food = player.getFoodData();
        if (food.getFoodLevel() < 20) {
            food.setFoodLevel(20);     // 항상 배부름
        }
        food.setExhaustion(0.0F);      // 피로(소모) 리셋 → 배고픔 안 깎임
    }
}
