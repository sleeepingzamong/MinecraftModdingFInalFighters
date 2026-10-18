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

    public static final RegistryObject<Item> HAMMER_THROWN = ITEMS.register("hammer_thrown",
        () -> new Item(new Item.Properties()));   // 날아가는 해머 (모델 표시용 — hammer_thrown.json으로 조절)

    public static final RegistryObject<Item> POWER_KNUCKLE = ITEMS.register("power_knuckle",
        () -> new PowerKnuckleItem(new Item.Properties().stacksTo(1)));   // 사이보그 무기

    public static final RegistryObject<Item> CYBER_SCYTHE = ITEMS.register("cyber_scythe",
        () -> new CyberScytheItem(new Item.Properties().stacksTo(1)));   // 해커 무기 (데이터 리퍼 낫)

    public static final RegistryObject<Item> BATTLE_AXE = ITEMS.register("battle_axe",
        () -> new BattleAxeItem(new Item.Properties().stacksTo(1)));   // 광전사 무기 (양손 도끼)

    public static final RegistryObject<Item> FIRE_BLADE = ITEMS.register("fire_blade",
        () -> new com.example.examplemod.item.FireBladeItem(new Item.Properties().stacksTo(1)));   // 예전 불사조 무기 (불칼, 은퇴)

    public static final RegistryObject<Item> FEATHER_FAN = ITEMS.register("feather_fan",
        () -> new com.example.examplemod.item.FireBladeItem(new Item.Properties().stacksTo(1)));   // 불사조 무기 (깃털 부채 — 불칼과 같은 능력치·화상)

    public static final RegistryObject<Item> PHOENIX_FEATHER = ITEMS.register("phoenix_feather",
        () -> new Item(new Item.Properties()));   // 날아가는 불꽃 깃털 (Z 투사체 모델 표시용)

    public static final RegistryObject<Item> SKY_BLOCK = ITEMS.register("sky_block",
        () -> new BlockItem(ModBlocks.SKY_BLOCK.get(), new Item.Properties()));   // 하늘 블록 (뒤를 숨기고 하늘만)

    // ===== 캐릭터 전용 장비 (선택 시 자동 착용 — 이름, 방어력, 부위) =====
    public static final RegistryObject<Item> MONKEY_CIRCLET = ITEMS.register("monkey_circlet",
        () -> new CharacterArmorItem("monkey", 4, net.minecraft.world.item.ArmorItem.Type.HELMET,
                new Item.Properties().stacksTo(1)));   // 손오공 — 긴고아 (금 머리테)
    public static final RegistryObject<Item> GAMBLER_FEDORA = ITEMS.register("gambler_fedora",
        () -> new CharacterArmorItem("gambler", 2, net.minecraft.world.item.ArmorItem.Type.HELMET,
                new Item.Properties().stacksTo(1)));   // 갬블러 — 중절모
    public static final RegistryObject<Item> SWORDSMAN_CREST = ITEMS.register("swordsman_crest",
        () -> new CharacterArmorItem("swordsman", 3, net.minecraft.world.item.ArmorItem.Type.HELMET,
                new Item.Properties().stacksTo(1)));   // 검사 — 사무라이 전립
    public static final RegistryObject<Item> HACKER_GOGGLES = ITEMS.register("hacker_goggles",
        () -> new CharacterArmorItem("hacker", 2, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                new Item.Properties().stacksTo(1)));   // 해커 — 글리치 후드집업 (몸)
    public static final RegistryObject<Item> HAMMER_MASK = ITEMS.register("hammer_mask",
        () -> new CharacterArmorItem("hammer", 6, net.minecraft.world.item.ArmorItem.Type.HELMET,
                new Item.Properties().stacksTo(1)));   // 해머 — 가면
    public static final RegistryObject<Item> CYBORG_CHESTPLATE = ITEMS.register("cyborg_chestplate",
        () -> new CharacterArmorItem("cyborg", 6, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                new Item.Properties().stacksTo(1)));   // 사이보그 — 외골격 흉갑
    public static final RegistryObject<Item> BERSERKER_PELT = ITEMS.register("berserker_pelt",
        () -> new CharacterArmorItem("berserker", 4, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                new Item.Properties().stacksTo(1)));   // 광전사 — 늑대 모피 갑주
    public static final RegistryObject<Item> DOJUK_HOODIE = ITEMS.register("dojuk_hoodie",
        () -> new CharacterArmorItem("dojuk", 2, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                new Item.Properties().stacksTo(1)));   // 도적 — 회백 후드집업
    public static final RegistryObject<Item> PHOENIX_WINGS = ITEMS.register("phoenix_wings",
        () -> new CharacterArmorItem("phoenix", 2, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                new Item.Properties().stacksTo(1)));   // 불사조 — 화염 날개 (등 뒤 펼쳐진 날개)
}