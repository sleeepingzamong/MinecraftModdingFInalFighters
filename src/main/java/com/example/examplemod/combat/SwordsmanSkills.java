package com.example.examplemod.combat;

import net.minecraft.server.level.ServerPlayer;

public class SwordsmanSkills implements CharacterSkills {

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick) {
        // Z: 연격 — 다음 평타 2회가 각각 한 번 더 타격 (판정은 SwordsmanEvents)
        SwordsmanEvents.startEcho(player);
    }

    @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        // X: 패링 — 머리 위 표식 3개, 공격을 1회씩 완전 무효
        SwordsmanEvents.startParry(player);
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // 궁: 강화 — 공격력·이동속도·공격속도 + 흡혈
        SwordsmanEvents.startEnhance(player);
    }
}
