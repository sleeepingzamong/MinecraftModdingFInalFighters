package com.example.examplemod.client;

import com.example.examplemod.item.KnifeItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.entity.player.Player;

// 바닐라 블록 부스러기(BLOCK) 파티클 공급자를 감싼 필터.
// 도적(단검 소지 플레이어) 발밑에서 생기는 달리기·착지 잔해를 스폰 단계에서 걸러낸다.
// (달리기 잔해는 각 클라이언트가 Entity.baseTick에서 직접 스폰해 서버로는 못 막음 —
//  그래서 클라이언트의 BLOCK 파티클 공급자 자체를 교체한다. 그 외 위치의 부스러기는 그대로 위임)
public class NoTraceBlockParticleProvider implements ParticleProvider<BlockParticleOption> {

    private final TerrainParticle.Provider vanilla = new TerrainParticle.Provider();

    @Override
    public Particle createParticle(BlockParticleOption option, ClientLevel level,
                                   double x, double y, double z,
                                   double dx, double dy, double dz) {
        for (Player p : level.players()) {
            if (p.getMainHandItem().getItem() instanceof KnifeItem
                    && Math.abs(x - p.getX()) < 1.2
                    && Math.abs(z - p.getZ()) < 1.2
                    && y > p.getY() - 0.6 && y < p.getY() + 0.8) {
                return null;   // 도적 발밑 잔해 → 스폰하지 않음
            }
        }
        return vanilla.createParticle(option, level, x, y, z, dx, dy, dz);
    }
}
