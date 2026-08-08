package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 바닐라 F키(양손 아이템 교체) 비활성화
//  - 가드 키(F)와 겹쳐 오작동하는 문제 해결
//  - 사이보그 왼손 너클이 실수로 옮겨지는 것도 방지
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class HandSwapDisabler {

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof Player) {
            event.setCanceled(true);
        }
    }
}
