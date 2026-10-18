package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ExampleMod.MODID);

    // 레지스트리 이름 "laser_charge" 는 sounds.json 의 키와 같아야 함.
    public static final RegistryObject<SoundEvent> LASER_CHARGE = SOUND_EVENTS.register("laser_charge",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ExampleMod.MODID, "laser_charge")));

    public static final RegistryObject<SoundEvent> BUDDA = SOUND_EVENTS.register("budda",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ExampleMod.MODID, "budda")));

    public static final RegistryObject<SoundEvent> REVOLVER_SHOT = SOUND_EVENTS.register("revolver_shot",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ExampleMod.MODID, "revolver_shot")));

    public static final RegistryObject<SoundEvent> COIN_TOSS = SOUND_EVENTS.register("coin_toss",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ExampleMod.MODID, "coin_toss")));

    public static final RegistryObject<SoundEvent> HACK_AMBIENCE = SOUND_EVENTS.register("hack_ambience",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ExampleMod.MODID, "hack_ambience")));

    // 불사조 부채 휘두르기 — Better Combat이 매 휘두름마다 재생 (weapon_attributes/feather_fan.json의 swing_sound).
    // BC는 소리를 레지스트리에서 찾으므로 sounds.json만으론 안 되고 여기 등록이 필요함. (명중 시 소리는 phoenix_attack 슬롯)
    public static final RegistryObject<SoundEvent> PHOENIX_SWING = SOUND_EVENTS.register("phoenix_swing",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ExampleMod.MODID, "phoenix_swing")));

    // 불사조 궁 장판 지속음 (모닥불 타닥타닥) — 클라 PhoenixZoneSound가 장판 동안 재생하고 장판이 사라지면 끔.
    // 시전 순간 한 번 나는 소리는 궁 슬롯 phoenix_ult.
    public static final RegistryObject<SoundEvent> PHOENIX_ZONE = SOUND_EVENTS.register("phoenix_zone",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ExampleMod.MODID, "phoenix_zone")));

    // ===== 캐릭터별 평타/스킬 사운드 슬롯 (8캐릭터 × attack/z/x/ult = 32개) =====
    // 지금은 전부 무음 ogg가 깔려 있음 — 나중에 sounds/skill/<이름>.ogg 파일만 교체하면
    // 재시작 없이 F3+T로 새 소리가 적용된다. (키: "monkey_z", "hammer_attack" 등)
    public static final java.util.Map<String, RegistryObject<SoundEvent>> SKILL_SOUNDS = new java.util.HashMap<>();

    static {
        String[] chars = {"monkey", "cyborg", "dojuk", "gambler", "swordsman", "hacker", "hammer", "berserker", "phoenix"};
        String[] slots = {"attack", "z", "x", "ult"};
        for (String c : chars) {
            for (String s : slots) {
                String name = c + "_" + s;
                SKILL_SOUNDS.put(name, SOUND_EVENTS.register(name,
                        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ExampleMod.MODID, name))));
            }
        }
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
