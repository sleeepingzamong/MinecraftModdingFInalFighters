package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

// 해커 X "버그 무시" 연출: 지속시간 동안 칩 바이러스 2마리가 플레이어 주변을 돈다
// (모두에게 보임 — 무적 상태의 시각적 텔레그래프)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class VirusOrbitRenderEvents {

    private static final ResourceLocation VIRUS_TEX =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/virus.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final float ICON_SIZE = 72.0F;      // 바이러스 크기 (기본 12의 6배 ≈ 1.8블록)
    private static final float ORBIT_RADIUS = 0.9F;    // 공전 반지름 (블록)
    private static final float ORBIT_HEIGHT = 1.0F;    // 몸통 높이
    private static final float DEG_PER_TICK = 18.0F;   // 공전 속도 (18°/틱 = 1초에 한 바퀴)

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (ClientBugIgnore.get(player.getId()) <= 0) {
            return;
        }

        float age = player.tickCount + event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(VIRUS_TEX));

        for (int i = 0; i < 2; i++) {
            float angleDeg = age * DEG_PER_TICK + i * 180.0F;
            double angle = Math.toRadians(angleDeg);
            float ox = (float) (Math.cos(angle) * ORBIT_RADIUS);
            float oz = (float) (Math.sin(angle) * ORBIT_RADIUS);

            pose.pushPose();
            pose.translate(ox, ORBIT_HEIGHT, oz);
            // 카메라가 아니라 공전 중심(플레이어)을 바라보며 돈다
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F - angleDeg));
            pose.scale(-0.025F, -0.025F, 0.025F);

            Matrix4f matrix = pose.last().pose();
            Matrix3f normal = pose.last().normal();
            float half = ICON_SIZE / 2.0F;
            vertex(vc, matrix, normal, -half, half, 0.0F, 1.0F);
            vertex(vc, matrix, normal, half, half, 1.0F, 1.0F);
            vertex(vc, matrix, normal, half, -half, 1.0F, 0.0F);
            vertex(vc, matrix, normal, -half, -half, 0.0F, 0.0F);

            pose.popPose();
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v) {
        vc.vertex(m, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientBugIgnore.tickAll();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBugIgnore.clearAll();
    }
}
