package com.example.examplemod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

// 광전사 분노 불꽃: 위로 타오르며 좌우로 살짝 흔들리다 사라짐
public class RageParticle extends TextureSheetParticle {

    protected RageParticle(ClientLevel level, double x, double y, double z,
                           double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.lifetime = 20 + this.random.nextInt(10);   // 1~1.5초
        this.gravity = 0.0F;
        this.friction = 0.92F;
        this.xd = dx + (this.random.nextDouble() - 0.5) * 0.04;
        this.yd = dy + 0.10 + this.random.nextDouble() * 0.05;   // 불길처럼 위로
        this.zd = dz + (this.random.nextDouble() - 0.5) * 0.04;
        this.quadSize = 0.22F + this.random.nextFloat() * 0.1F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        // 좌우로 흔들리는 불길 + 수명 끝으로 갈수록 작아지고 투명해짐
        this.xd += Math.sin((this.age + this.x) * 0.6) * 0.002;
        float life = (float) this.age / this.lifetime;
        this.alpha = 1.0F - life;
        this.quadSize *= 0.985F;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;   // 불꽃이니까 어둠 속에서도 밝게
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
            RageParticle particle = new RageParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
