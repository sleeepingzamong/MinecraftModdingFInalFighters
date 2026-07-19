package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 바닐라 HUD 요소 숨기기: 배고픔 바 + 경험치바를 안 그린다(경험치바는 차징 바로 대체).
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class HudHiderEvents {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        var id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
            event.setCanceled(true);        // 배고픔 바 안 그림
        }
        if (id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            event.setCanceled(true);        // 경험치바 안 그림 (차징 바로 대체)
        }
    }
}
