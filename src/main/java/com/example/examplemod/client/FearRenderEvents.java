package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

// 광전사 궁 공포: 걸린 대상(플레이어·몹) 머리 위에 귀신 아이콘을 띄운다 (둥둥 떠다니는 연출)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class FearRenderEvents {

    private static final ResourceLocation GHOST_TEX =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/ghost.png");
    private static final int FULL_BRIGHT = 0xF000F0;   // 어두워도 잘 보이게 최대 밝기

    private static final float ICON_SIZE = 14.0F;      // 아이콘 크기 (이름표 글자 기준 px)

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (ClientFearData.get(entity.getId()) <= 0) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // 이름표보다 살짝 위, 항상 카메라를 바라보게 (패링 표식과 같은 방식)
        pose.translate(0.0D, entity.getBbHeight() + 0.8D, 0.0D);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);

        // 귀신답게 위아래로 살랑살랑 떠다니는 연출
        float bob = (float) Math.sin((entity.tickCount + event.getPartialTick()) / 8.0F) * 1.5F;

        VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(GHOST_TEX));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        drawIcon(vc, matrix, normal, -ICON_SIZE / 2.0F, -ICON_SIZE / 2.0F + bob);
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

    // 공포 남은 시간 카운트다운 (클라 틱)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientFearData.tickDown();
        }
    }

    // 월드에서 나가면 기억을 비움 (다음 월드에서 남의 엔티티 ID와 섞이지 않게)
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFearData.clearAll();
        ClientRageData.clear();   // 광전사 분노 HUD도 초기화
    }
}
