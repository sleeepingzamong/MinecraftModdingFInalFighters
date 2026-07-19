package com.example.examplemod;

import com.example.examplemod.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModParticles.register(modEventBus);
        // 분신 몹 능력치 등록(MOD 버스). 없으면 분신 스폰 시 크래시한다.
        modEventBus.addListener(ModEntities::onAttributeCreation);
        ModMessages.register();
    }

}
