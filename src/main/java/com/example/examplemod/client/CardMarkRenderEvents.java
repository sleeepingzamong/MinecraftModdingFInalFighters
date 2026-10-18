package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
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

// 갬블러 블랙잭 카드: 대상 머리 위 카드 아이콘 + "N / 21" — 시전자 화면에만 그림
// (공포 귀신 아이콘과 같은 클라이언트 렌더 방식)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class CardMarkRenderEvents {

    private static final ResourceLocation CARD_TEX =
            new ResourceLocation(ExampleMod.MODID, "textures/item/blackjack.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float ICON_SIZE = 10.0F;

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!ClientCardMark.isMarked(entity.getId())) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0D, entity.getBbHeight() + 0.7D, 0.0D);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();

        // 숫자 "N / 21" (금색 굵게, 벽 너머로도 보임)
        Font font = Minecraft.getInstance().font;
        Component text = Component.literal(ClientCardMark.getSum() + " / 21")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        float textX = -font.width(text) / 2.0F + ICON_SIZE / 2.0F + 1.0F;   // 아이콘 폭만큼 오른쪽으로
        font.drawInBatch(text, textX, -4.0F, 0xFFFFFF, false, matrix,
                event.getMultiBufferSource(), Font.DisplayMode.SEE_THROUGH, 0x40000000, FULL_BRIGHT);

        // 카드 아이콘 (텍스트 왼쪽에, 살랑살랑)
        float bob = (float) Math.sin((entity.tickCount + event.getPartialTick()) / 9.0F) * 1.2F;
        float iconX = textX - ICON_SIZE - 2.0F;
        VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(CARD_TEX));
        drawIcon(vc, matrix, normal, iconX, -ICON_SIZE / 2.0F - 1.0F + bob);

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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientCardMark.tickDown();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCardMark.clearAll();
    }
}
