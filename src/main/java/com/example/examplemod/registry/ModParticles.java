package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ExampleMod.MODID);

    // 도박사 룰렛 결과 파티클 (텍스처는 particles/*.json이 연결)
    public static final RegistryObject<SimpleParticleType> SEVEN =
            PARTICLE_TYPES.register("seven", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> SKULL =
            PARTICLE_TYPES.register("skull", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
