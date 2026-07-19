package com.example.examplemod.combat;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import net.minecraft.server.level.ServerPlayer;

public class HammerSkills implements CharacterSkills{

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick ){

    }

     @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        // 토글 종료·망치 확인은 SkillManager 사전 체크에서 끝남 — 여기 도달 = 회전 시작
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .ifPresent(data -> HammerSpinEvents.startSpin(player, data));
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // player.sendSystemMessage(Component.literal("사이보그 궁극기 사용! 차징: " + chargeTick + "틱"));
    }
}
