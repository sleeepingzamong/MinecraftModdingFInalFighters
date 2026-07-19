package com.example.examplemod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

// 도박사 파티클(7·해골): 위로 두둥실 떠오르며 서서히 사라짐
public class GamblerParticle extends TextureSheetParticle {

    protected GamblerParticle(ClientLevel level, double x, double y, double z,
                              double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.lifetime = 30;          // 1.5초
        this.gravity = 0.0F;         // 안 떨어짐
        this.friction = 0.95F;       // 서서히 감속
        this.xd = dx;
        this.yd = dy + 0.06;         // 위로 떠오름
        this.zd = dz;
        this.quadSize = 0.15F;        // 표시 크기
        this.hasPhysics = false;     // 블럭에 안 걸림
    }

    @Override
    public void tick() {
        super.tick();
        // 수명 끝으로 갈수록 투명해짐
        this.alpha = 1.0F - (float) this.age / this.lifetime;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            GamblerParticle particle = new GamblerParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
