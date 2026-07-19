package com.example.examplemod.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;


import com.example.examplemod.capability.PlayerCombatDataProvider;

public class StaminaManager {

  

    public static void resetStamina(ServerPlayer player, CharacterType characterType) {
        int max = CharacterStats.getMaxStamina(characterType);
         player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
               .ifPresent(data -> data.setStamina(max));
        CombatSync.syncStats(player);
    }

    public static int getStamina(ServerPlayer player) {
        CharacterType characterType = PlayerCharacterManager.getCharacter(player);
        int maxStamina = CharacterStats.getMaxStamina(characterType);

        return player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                      .map(data -> data.getStamina())
                      .orElse(maxStamina);
    }

    public static boolean consumeStamina(ServerPlayer player, int amount) {
        int currentStamina = getStamina(player);

        if (currentStamina < amount) {
            player.sendSystemMessage(Component.literal("스테미나가 부족합니다."));
            return false;
        }
        int newStamina = currentStamina - amount;
         player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
               .ifPresent(data -> data.setStamina(newStamina));

        CombatSync.syncStats(player);

        return true;
    }
    public static void recoverStamina(ServerPlayer player, int amount) {
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .ifPresent(data -> recoverStamina(player, data, amount));
    }

    // 이미 조회한 data로 처리 (틱 허브 경로 — capability 재조회 없음)
    public static void recoverStamina(ServerPlayer player, PlayerCombatData data, int amount) {
        int maxStamina = CharacterStats.getMaxStamina(data.getCharacter());
        int currentStamina = data.getStamina();

        if (currentStamina >= maxStamina) {
            return;   // 가득 차 있으면 아무것도 안 함 (패킷도 없음)
        }

        data.setStamina(Math.min(currentStamina + amount, maxStamina));
        CombatSync.syncStats(player);
    }
}