package com.example.examplemod.client;

import com.example.examplemod.entity.SkillProjectileEntity;
import com.example.examplemod.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public class SkillProjectileRenderer extends EntityRenderer<SkillProjectileEntity> {
    private final ItemRenderer itemRenderer;

    public SkillProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SkillProjectileEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // 비행 방향으로 고정 (카메라 billboard 제거)
        float yaw = entity.getLaunchYaw();
        float pitch = entity.getLaunchPitch();

        // === 모델 방향 보정: 안 맞으면 이 숫자만 바꾸세요 ===
        float YAW_OFFSET   = 180.0F;  // 좌우(손바닥 방향): 내 쪽 향하면 180
        float PITCH_OFFSET = 0.0F;    // 위아래 기울기
        float ROLL_OFFSET  = 0.0F;    // 상하 뒤집힘이면 180
        // ===============================================

        // 1) 비행 방향으로 조준 (보정값 안 섞음 → pitch 안 틀어짐)
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        // 2) 모델 자체 방향 보정 (아트 정렬용 — 조준엔 영향 없음)
        poseStack.mulPose(Axis.YP.rotationDegrees(YAW_OFFSET));
        poseStack.mulPose(Axis.XP.rotationDegrees(PITCH_OFFSET));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ROLL_OFFSET));
        // 단검·총알은 모델이 세로라 90도 눕혀서 (끝이 앞을 보게)
        if (entity.getItem().is(ModItems.KNIFE.get()) || entity.getItem().is(ModItems.BULLET.get())) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        }
        // 카드는 반대 방향으로 눕혀서 앞면이 땅을 보게
        if (entity.getItem().is(ModItems.CARD.get())) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SkillProjectileEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;   // 아이템 렌더라 실제로 안 쓰임
    }
}