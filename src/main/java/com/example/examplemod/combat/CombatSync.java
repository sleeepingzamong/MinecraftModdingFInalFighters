package com.example.examplemod.combat;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.network.CombatStatsS2CPacket;
import com.example.examplemod.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;

public class CombatSync {

    // 현재 스탯을 모아서 그 플레이어에게 전송 (capability 조회 1번, enum 그대로 전송 — 문자열 조립 없음)
    public static void syncStats(ServerPlayer player) {
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            int maxStamina = CharacterStats.getMaxStamina(data.getCharacter());
            ModMessages.sendToPlayer(new CombatStatsS2CPacket(
                    data.getStamina(), maxStamina, data.getUltimateGauge(),
                    data.getCopiedCharacter(), data.getCopiedSlot(), data.getCharacter(),
                    data.getCoinType(0), data.getCoinType(1), data.getCoinType(2),
                    data.getNextGunDamage()), player);
        });
    }
}
