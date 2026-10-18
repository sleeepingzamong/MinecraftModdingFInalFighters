package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Q(아이템 버리기) 비활성화.
// Minecraft.tick()은 handleKeybinds()에서 keyDrop의 눌린 횟수를 읽어 버리기를 실행한다.
// 그보다 먼저 실행되는 ClientTickEvent START에서 그 횟수를 미리 소진시키면
// 서버로 버리기 패킷 자체가 나가지 않는다 → 아이템이 사라질 위험이 없다.
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class DropKeyDisableEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) {
            return;
        }
        while (mc.options.keyDrop.consumeClick()) {
            // 입력을 삼키기만 한다 (Ctrl+Q 스택 통째로 버리기도 같은 키라 함께 막힘)
        }
    }
}
