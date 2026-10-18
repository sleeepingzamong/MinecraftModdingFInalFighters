package com.example.examplemod.combat;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Vector3f;

// 빈사 상태 (체력 6 이하): 발밑에 작은 핏방울 파티클 (모두에게 보임 — 빈사 어필)
// 심장박동 소리·붉은 비네트는 클라이언트가 자기 체력을 보고 직접 처리 (client.LowHealthEffects)
public class LowHealthEvents {

    public static final float THRESHOLD = 6.0F;   // 빈사 기준 체력 (하트 3개)

    private static final DustParticleOptions BLOOD_DRIP =
            new DustParticleOptions(new Vector3f(0.55F, 0.04F, 0.04F), 0.7F);

    // PlayerTickHub가 매 틱 호출
    public static void tick(ServerPlayer player, ServerLevel level) {
        if (level.getGameTime() % 10 != 0) {
            return;   // 0.5초마다
        }
        if (player.isSpectator() || player.isCreative()
                || !player.isAlive() || player.getHealth() > THRESHOLD) {
            return;
        }
        level.sendParticles(BLOOD_DRIP,
                player.getX(), player.getY() + 0.1D, player.getZ(),
                2, 0.2D, 0.02D, 0.2D, 0.0D);
    }
}
