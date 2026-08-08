package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 해커 궁 "시스템 해킹"을 당한 쪽의 클라이언트 연출:
//  - 이동키 반전 (W↔S, A↔D)
//  - 화면 글리치 오버레이 (노이즈 바 + SYSTEM HACKED 깜빡임)
//  - 간헐적 시점 흔들림
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class HackerClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientHackState.tickDown();
        }
    }

    // 이동키 반전 — 입력값 부호를 뒤집으면 W가 뒤로, A가 오른쪽으로 감
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientHackState.isHacked()) {
            return;
        }
        event.getInput().forwardImpulse = -event.getInput().forwardImpulse;
        event.getInput().leftImpulse = -event.getInput().leftImpulse;
    }

    // 간헐적 시점 흔들림 (10틱 중 3틱만 지지직)
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (!ClientHackState.isHacked()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.tickCount % 10 >= 3) {
            return;
        }
        RandomSource random = RandomSource.create(mc.player.tickCount);
        event.setYaw(event.getYaw() + (random.nextFloat() - 0.5F) * 4.0F);
        event.setPitch(event.getPitch() + (random.nextFloat() - 0.5F) * 2.0F);
    }

    // 화면 글리치 오버레이
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientHackState.isHacked()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // 노이즈 바: 매 틱 패턴이 바뀌는 반투명 가로줄들 (시안·마젠타·흰색) — 개수·두께·진하기 강화
        RandomSource random = RandomSource.create(mc.player.tickCount);
        for (int i = 0; i < 22; i++) {
            int y = random.nextInt(h);
            int barH = 1 + random.nextInt(4);
            int x = random.nextInt(w / 2);
            int barW = w / 4 + random.nextInt(w / 2);
            int color = switch (random.nextInt(3)) {
                case 0 -> 0x7000F0FF;   // 시안
                case 1 -> 0x70FF00E0;   // 마젠타
                default -> 0x60FFFFFF;  // 흰 노이즈
            };
            g.fill(x, y, x + barW, y + barH, color);
        }

        // 굵은 글리치 블록 (화면이 뭉텅뭉텅 깨지는 느낌)
        for (int i = 0; i < 5; i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            int blockW = 10 + random.nextInt(50);
            int blockH = 4 + random.nextInt(12);
            int color = random.nextBoolean() ? 0x4800F0FF : 0x48FF00E0;
            g.fill(x, y, x + blockW, y + blockH, color);
        }

        // 간헐적 전체 화면 플래시 (지지직 번쩍)
        if (random.nextInt(8) == 0) {
            g.fill(0, 0, w, h, random.nextBoolean() ? 0x2800F0FF : 0x28FF00E0);
        }

        // 상하 가장자리 시안 띠 (더 두껍게)
        g.fill(0, 0, w, 4, 0x9000F0FF);
        g.fill(0, h - 4, w, h, 0x9000F0FF);

        // "SYSTEM HACKED" 깜빡임 (2배 크기, 더 빠르게)
        if ((mc.player.tickCount / 3) % 2 == 0) {
            g.pose().pushPose();
            g.pose().scale(2.0F, 2.0F, 1.0F);
            g.drawCenteredString(mc.font, "SYSTEM HACKED", w / 4, 12, 0xFF00F0FF);
            g.pose().popPose();
        }
    }

    // 월드에서 나가면 해킹 상태 초기화
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHackState.set(0);
    }
}
