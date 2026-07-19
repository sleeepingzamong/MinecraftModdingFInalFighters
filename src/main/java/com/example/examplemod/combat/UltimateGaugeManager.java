package com.example.examplemod.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.example.examplemod.capability.PlayerCombatDataProvider;


public class UltimateGaugeManager {


    public static final int MAX_GAUGE = 100;

    public static int getGauge(ServerPlayer player) {
        return player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                     .map(data -> data.getUltimateGauge())
                      .orElse(0);

    }

    public static void addGauge(ServerPlayer player, int amount) {
        int currentGauge = getGauge(player);
        int newGauge = Math.min(currentGauge + amount, MAX_GAUGE);
        if (newGauge == currentGauge) {
            return;   // 이미 가득 참 등 값이 안 변하면 동기화 패킷도 안 보냄
        }

        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
               .ifPresent(data -> data.setUltimateGauge(newGauge));

        CombatSync.syncStats(player);
    }

    public static boolean consumeGauge(ServerPlayer player, int amount) {
        int currentGauge = getGauge(player);

        if (currentGauge < amount) {
            player.sendSystemMessage(Component.literal("필살기 게이지가 부족합니다."));
            return false;
        }

        int newGauge = currentGauge - amount;
         player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
               .ifPresent(data -> data.setUltimateGauge(newGauge));
        CombatSync.syncStats(player);
        return true;
    }

    public static void resetGauge(ServerPlayer player) {
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .ifPresent(data -> data.setUltimateGauge(0));
        CombatSync.syncStats(player);
    }
}