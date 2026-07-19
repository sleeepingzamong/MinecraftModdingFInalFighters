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

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
