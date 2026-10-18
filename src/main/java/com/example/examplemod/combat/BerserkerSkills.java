package com.example.examplemod.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class BerserkerSkills implements CharacterSkills {

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick) {
        // Z: 돌진 찍기 — 전방 돌진, 부딪힌 적 데미지 + 넉백
        if (player.level() instanceof ServerLevel level) {
            BerserkerEvents.chargeStrike(player, level);
        }
    }

    @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        // X: 피의 격노 — 체력을 바치고 공속·이속 증가
        if (player.level() instanceof ServerLevel level) {
            BerserkerEvents.bloodFrenzy(player, level);
        }
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // 궁: 광폭화 — 잃은 체력 비례 공격력 + 피해 감소 + 주변 공포
        if (player.level() instanceof ServerLevel level) {
            BerserkerEvents.rage(player, level);
        }
    }
}
