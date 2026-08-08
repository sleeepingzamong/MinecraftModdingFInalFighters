package com.example.examplemod.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class HackerSkills implements CharacterSkills {

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick) {
        // Z: 롤백 — 3초 전 위치로 순간이동 + 떠난 자리에 글리치 잔상(스턴)
        if (player.level() instanceof ServerLevel level) {
            HackerEvents.rollback(player, level);
        }
    }

    @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        // X: 버그 무시 — 0.5초 동안 모든 공격 무효
        if (player.level() instanceof ServerLevel level) {
            HackerEvents.bugIgnore(player, level);
        }
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // 궁: 시스템 해킹 — 바라보는 플레이어의 이동키 반전 + 화면 글리치
        if (player.level() instanceof ServerLevel level) {
            HackerEvents.systemHack(player, level);
        }
    }
}
