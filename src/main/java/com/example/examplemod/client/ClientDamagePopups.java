package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.config.FFConfig;
import com.example.examplemod.network.DamagePopupS2CPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// 데미지 숫자 팝업 (월드 공간 빌보드 렌더).
// 지형까지만 그려진 시점에 깊이 판정으로 그림 → 벽 뒤에서는 자연스럽게 가려지고,
// 월드에 박힌 텍스트라 원근·시차가 자연스럽다.
// (물리적으로 카메라와 숫자 사이에 낀 빔·엔티티는 가릴 수 있음 — 공간적으로 올바른 동작)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ClientDamagePopups {

    private static final int FULL_BRIGHT = 0xF000F0;

    private static class Popup {
        final double x, y0, z;
        final String text;
        final ChatFormatting color;
        int age = 0;

        Popup(double x, double y0, double z, String text, ChatFormatting color) {
            this.x = x;
            this.y0 = y0;
            this.z = z;
            this.text = text;
            this.color = color;
        }
    }

    private static final List<Popup> POPUPS = new ArrayList<>();

    // kind: DamagePopupS2CPacket.DAMAGE(금색) / HEADSHOT(빨강) / HEAL(초록, "+숫자")
    public static void add(double x, double y, double z, float amount, byte kind) {
        String num = amount == Math.floor(amount)
                ? String.valueOf((int) amount)
                : String.format("%.1f", amount);
        ChatFormatting color;
        if (kind == DamagePopupS2CPacket.HEAL) {
            num = "+" + num;
            color = ChatFormatting.GREEN;
        } else {
            color = kind == DamagePopupS2CPacket.HEADSHOT ? ChatFormatting.RED : ChatFormatting.GOLD;
        }
        POPUPS.add(new Popup(x, y, z, num, color));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        int lifetime = FFConfig.POPUP_LIFETIME.get();
        Iterator<Popup> it = POPUPS.iterator();
        while (it.hasNext()) {
            if (++it.next().age >= lifetime) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        // 컷아웃 블록까지 그려진 직후 — 행렬이 유효하고, 깊이 버퍼에 지형이 있어 벽 가림이 자연스러움
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS || POPUPS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        float scale = (float) (double) FFConfig.POPUP_SCALE.get();
        double rise = FFConfig.POPUP_RISE.get();
        float partialTick = event.getPartialTick();

        for (Popup p : POPUPS) {
            double y = p.y0 + (p.age + partialTick) * rise;

            pose.pushPose();
            pose.translate(p.x - cam.x, y - cam.y, p.z - cam.z);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(-0.025F * scale, -0.025F * scale, 0.025F * scale);

            Component text = Component.literal(p.text).withStyle(p.color, ChatFormatting.BOLD);
            Matrix4f matrix = pose.last().pose();
            font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFFFFFF, false,
                    matrix, buffer, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            pose.popPose();
        }
        buffer.endBatch();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        POPUPS.clear();
    }
}
