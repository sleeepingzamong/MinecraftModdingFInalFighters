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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

// 검사 패링 표식: 플레이어 머리 위에 남은 표식만큼 칼 아이콘을 띄운다 (이름표처럼 카메라를 향함)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class ParryMarkRenderEvents {

    private static final ResourceLocation MARK_TEX =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/samurai_x.png");   // 사용자가 그린 교차 검 표식
    private static final int FULL_BRIGHT = 0xF000F0;   // 어두워도 잘 보이게 최대 밝기

    private static final float ICON_SIZE = 10.0F;      // 아이콘 크기 (이름표 글자 기준 px)
    private static final float ICON_GAP = 2.0F;        // 아이콘 사이 간격

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        int marks = ClientParryMarks.get(player.getId());
        if (marks <= 0) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // 이름표보다 살짝 위, 항상 카메라를 바라보게 (이름표 렌더와 같은 방식)
        pose.translate(0.0D, player.getBbHeight() + 0.75D, 0.0D);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);

        float totalW = marks * ICON_SIZE + (marks - 1) * ICON_GAP;
        float startX = -totalW / 2.0F;
        VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(MARK_TEX));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();

        for (int i = 0; i < marks; i++) {
            drawIcon(vc, matrix, normal, startX + i * (ICON_SIZE + ICON_GAP), -ICON_SIZE / 2.0F);
        }
        pose.popPose();
    }

    private static void drawIcon(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y) {
        vertex(vc, m, n, x, y + ICON_SIZE, 0.0F, 1.0F);
        vertex(vc, m, n, x + ICON_SIZE, y + ICON_SIZE, 1.0F, 1.0F);
        vertex(vc, m, n, x + ICON_SIZE, y, 1.0F, 0.0F);
        vertex(vc, m, n, x, y, 0.0F, 0.0F);
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

    // 월드에서 나가면 표식 기억을 비움 (다음 월드에서 남의 엔티티 ID와 섞이지 않게)
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientParryMarks.clearAll();
    }
}
