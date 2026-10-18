package com.example.examplemod;

import com.example.examplemod.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.example.examplemod.registry.ModBlockEntities;
import com.example.examplemod.registry.ModBlocks;
import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.registry.ModParticles;
import com.example.examplemod.registry.ModSounds;
import com.example.examplemod.network.ModMessages;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "examplemod";

    public ExampleMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModParticles.register(modEventBus);
        com.example.examplemod.registry.ModEffects.register(modEventBus);
        // 분신 몹 능력치 등록(MOD 버스). 없으면 분신 스폰 시 크래시한다.
        modEventBus.addListener(ModEntities::onAttributeCreation);
        ModMessages.register();

        // 실시간 설정 (config/examplemod-common.toml — 게임 켠 채 수정·저장하면 자동 반영)
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                com.example.examplemod.config.FFConfig.SPEC);
    }

}
