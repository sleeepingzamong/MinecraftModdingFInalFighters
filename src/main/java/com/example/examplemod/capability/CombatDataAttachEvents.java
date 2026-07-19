package com.example.examplemod.capability;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class CombatDataAttachEvents {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        // 플레이어가 아니면 무시
        if (!(event.getObject() instanceof Player)) {
            return;
        }

        // 이미 사물함이 붙어 있으면 또 붙이지 않음 (중복 방지)
        if (event.getObject().getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).isPresent()) {
            return;
        }

        // 사물함을 이름표와 함께 부착
        event.addCapability(
                new ResourceLocation(ExampleMod.MODID, "player_combat_data"),
                new PlayerCombatDataProvider()
        );
    }
}