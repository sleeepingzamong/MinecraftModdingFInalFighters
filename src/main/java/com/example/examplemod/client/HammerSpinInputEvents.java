package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 해머 회전 베기 중 좌클릭(평타) 입력을 클라이언트에서부터 차단
// — 팔 휘두르기·Better Combat 공격 모션 자체가 안 나오게 (서버 쪽 데미지 차단은 HammerSpinEvents)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class HammerSpinInputEvents {

    // 1차 방어선: 마우스 버튼 입력 자체를 차단
    // (Better Combat이 startAttack 맨 앞에 믹스인으로 끼어들어 아래 이벤트보다 먼저 클릭을
    //  가로채기 때문에, 그보다 앞 단계인 원시 마우스 입력에서 끊어야 BC 공격 모션이 안 나온다)
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !ClientSpinState.isSpinning(player)) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;   // 화면(채팅·인벤토리) 열려 있으면 정상 처리
        }
        if (Minecraft.getInstance().options.keyAttack.matchesMouse(event.getButton())) {
            event.setCanceled(true);   // 공격 클릭 무시 → BC·바닐라 모두 클릭을 못 봄
        }
    }

    // 2차 방어선: 공격 키가 마우스가 아닌 키보드에 바인딩된 경우
    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;   // 공격 입력만
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !ClientSpinState.isSpinning(player)) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);   // 팔도 안 휘두름 — 회전 모션 유지
    }

    // 3차 방어선: 공격 키를 누른 채로 회전을 시작해도 눌림 상태가 유지되지 않게 매 틱 강제 해제
    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !ClientSpinState.isSpinning(mc.player)) {
            return;
        }
        mc.options.keyAttack.setDown(false);
        while (mc.options.keyAttack.consumeClick()) {
        }
    }
}
