package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class ChargeZoomEvents {

    // 1) 화면 확대 — 차징 중 FOV를 줄임
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        int chargeTick = ClientSkillKeyEvents.getActiveChargeTick();
        if (chargeTick <= 0) {
            return;
        }

        int max = ClientSkillKeyEvents.getMaxChargeTick();
        double progress = Math.min(1.0, (double) chargeTick / max);

        double zoom = 1.0 - 0.4 * progress;   // 차징할수록 1.0 → 0.6
        event.setFOV(event.getFOV() * zoom);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (ClientSkillKeyEvents.getActiveChargeTick()  > 0) {
            event.getInput().forwardImpulse *= 0.4F;   // 앞뒤
            event.getInput().leftImpulse *= 0.4F;      // 좌우
        }
    }
}