package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 투명 상태 플레이어는 렌더링 자체를 취소 → 몸·손 아이템·갑옷·이름표까지 완전 투명
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class PlayerInvisRenderEvents {

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().isInvisible()) {
            event.setCanceled(true);
        }
    }
}
