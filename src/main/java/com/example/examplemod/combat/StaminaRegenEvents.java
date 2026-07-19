package com.example.examplemod.combat;

import net.minecraft.server.level.ServerPlayer;

// 스태미나 자동 재생 (PlayerTickHub가 매 틱 호출 — 이미 조회한 data를 받아 재조회 없음)
public class StaminaRegenEvents {

    private static final int REGEN_INTERVAL = 35;   // 이 틱마다 스태미나 1 회복

    public static void tick(ServerPlayer player, PlayerCombatData data) {
        if (data.isHammerSpinning()) {
            data.setStaminaRegenTimer(0);   // 회전 베기 중엔 회복 없음 (타이머도 정지)
            return;
        }

        int timer = data.getStaminaRegenTimer() + 1;
        if (timer >= REGEN_INTERVAL) {
            StaminaManager.recoverStamina(player, data, 1);
            timer = 0;
        }
        data.setStaminaRegenTimer(timer);
    }
}
