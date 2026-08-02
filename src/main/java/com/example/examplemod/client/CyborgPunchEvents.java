package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.PowerKnuckleItem;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.PunchC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 사이보그 너클 조작: 좌클릭 = 왼손 펀치, 우클릭 = 오른손 펀치
//  - 블록을 조준 중일 땐 바닐라 그대로 (좌클릭 채굴, 우클릭 문·상자 등 상호작용 유지)
//  - 너클이 아닌 아이템(음식 등)을 들면 완전히 바닐라 동작
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class CyborgPunchEvents {

    private static boolean useHeld = false;   // 우클릭 꾹 누름의 연속 발동 방지 (한 번 누름 = 한 펀치)

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // 너클을 들고 있을 때만 개입
        if (!(mc.player.getMainHandItem().getItem() instanceof PowerKnuckleItem)) {
            return;
        }
        // 블록 조준 중엔 바닐라 유지 (채굴·문·상자·버튼)
        HitResult hit = mc.hitResult;
        boolean lookingAtBlock = hit != null && hit.getType() == HitResult.Type.BLOCK;

        if (event.isAttack()) {
            if (lookingAtBlock) {
                return;   // 좌클릭 채굴 그대로
            }
            event.setCanceled(true);
            event.setSwingHand(false);   // 스윙은 서버가 왼팔로 브로드캐스트
            ModMessages.sendToServer(new PunchC2SPacket(true));    // 왼손 펀치
        } else if (event.isUseItem()) {
            if (lookingAtBlock) {
                return;   // 우클릭 상호작용 그대로
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!useHeld) {
                useHeld = true;   // 꾹 눌러도 1회만 — 다시 누르려면 손을 떼야 함
                ModMessages.sendToServer(new PunchC2SPacket(false));   // 오른손 펀치
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (useHeld && !mc.options.keyUse.isDown()) {
            useHeld = false;   // 우클릭에서 손을 뗌 → 다음 펀치 가능
        }
    }
}
