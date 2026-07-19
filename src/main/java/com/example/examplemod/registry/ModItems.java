package com.example.examplemod.registry;

import com.example.examplemod.item.*;
import com.example.examplemod.ExampleMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExampleMod.MODID);

    public static final RegistryObject<Item> RUBY = ITEMS.register("ruby",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RUBY_BLOCK = ITEMS.register("ruby_block",
            () -> new BlockItem(ModBlocks.RUBY_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHARACTER_SELECT_TICKET = ITEMS.register("character_select_ticket",
            () -> new CharacterSelectItem(new Item.Properties()));

            public static final RegistryObject<Item> RUYI_JINGU_BANG = ITEMS.register("ruyi_jingu_bang",
        () -> new RuyiJinguBangItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
    public static final RegistryObject<Item> KATANA = ITEMS.register("katana",
        () -> new KatanaItem(new Item.Properties()));

    public static final RegistryObject<Item> BUDDA = ITEMS.register("budda",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> KNIFE = ITEMS.register("knife",
        () -> new KnifeItem(new Item.Properties()));

    public static final RegistryObject<Item> LASER = ITEMS.register("laser",
        () -> new Item(new Item.Properties()));   // 레이저 빔 조각 (모델 표시용)

    public static final RegistryObject<Item> REVOLVER = ITEMS.register("revolver",
        () -> new RevolverItem(new Item.Properties().stacksTo(1)));   // 도박사 총 (겹침 불가)

    public static final RegistryObject<Item> BULLET = ITEMS.register("bullet",
        () -> new Item(new Item.Properties()));   // 날아가는 총알 (모델 표시용)

    public static final RegistryObject<Item> CARD = ITEMS.register("card",
        () -> new Item(new Item.Properties()));   // 블랙잭 카드 (모델 표시용)

    public static final RegistryObject<Item> BLACKJACK = ITEMS.register("blackjack",
        () -> new Item(new Item.Properties()));   // 카드 맞은 대상 몸에 뜨는 표식 (모델 표시용)

    public static final RegistryObject<Item> HAMMER = ITEMS.register("hammer",
        () -> new HammerItem(new Item.Properties().stacksTo(1)));   // 해머 캐릭터 무기 (아직 스탯 없음)
}