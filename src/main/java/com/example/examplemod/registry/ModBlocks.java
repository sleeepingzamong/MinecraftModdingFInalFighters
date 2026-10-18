package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.SkyBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ExampleMod.MODID);

    public static final RegistryObject<Block> RUBY_BLOCK = BLOCKS.register("ruby_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
            ));

    // 하늘 블록: 뒤의 지형을 가리고 하늘만 보이게 하는 연출용 블록 (맵/아레나 경계)
    public static final RegistryObject<Block> SKY_BLOCK = BLOCKS.register("sky_block",
            () -> new SkyBlock(BlockBehaviour.Properties.of()
                    .strength(0.5f)
                    .sound(SoundType.GLASS)
                    .noLootTable()
            ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}