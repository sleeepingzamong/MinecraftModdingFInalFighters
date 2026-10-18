package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 가드 중·스턴 중 좌클릭(공격) 입력을 클라이언트에서부터 차단 —
// 팔 휘두르기·Better Combat 공격 모션·블록 캐기·리볼버 사격까지 아예 시작되지 않음
// (서버 쪽 데미지 무효화는 GuardEvents·StunManager가 이미 처리 — 이건 모션/판정 차단)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class GuardInputBlockEvents {

    private static boolean blocksAttack() {
        return ClientSkillKeyEvents.isGuardingNow() || ClientSkillKeyEvents.isStunnedNow();
    }

    // 1차 방어선: 원시 마우스 입력 차단 (Better Combat보다 앞 단계)
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !blocksAttack()) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;   // 화면(채팅·인벤토리) 열려 있으면 정상 처리
        }
        if (Minecraft.getInstance().options.keyAttack.matchesMouse(event.getButton())) {
            event.setCanceled(true);
        }
    }

    // 2차 방어선: 공격 키가 키보드에 바인딩된 경우
    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !blocksAttack()) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    // 3차 방어선: 공격 키를 "누른 채로" 가드를 켜면 눌림 상태가 유지되며 연속 공격이 이어짐 —
    // 가드 중에는 매 틱 공격 키를 강제로 뗀 상태로 만들어 진행 중인 공격도 끊는다
    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !blocksAttack()) {
            return;
        }
        mc.options.keyAttack.setDown(false);
        while (mc.options.keyAttack.consumeClick()) {
        }
    }
}
