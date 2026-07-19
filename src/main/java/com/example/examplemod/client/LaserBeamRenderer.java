package com.example.examplemod.client;

import com.example.examplemod.entity.LaserBeamEntity;
import com.example.examplemod.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// 레이저 빔: 발사자 시선을 따라 1칸짜리 laser 모델을 벽까지 이어 그림 (통짜 빔처럼 보임)
public class LaserBeamRenderer extends EntityRenderer<LaserBeamEntity> {

    private static final double RANGE = 30.0;          // LaserBeamEvents.RANGE와 같게 유지
    private static final int FULL_BRIGHT = 15728880;   // 자체발광 (어두워도 밝게)
    private static final float BEAM_SCALE = 1.0F;      // 빔 굵기 배율 (길이는 그대로)
    private static final double BODY_OFFSET = 0.45;    // 눈보다 이만큼 아래(가슴)에서 발사
    private static final double GROW_SPEED = 3.7;      // 발사 시 빔 전진 속도 (칸/틱 = 60칸/초)
    private static final double FLOW_SPEED = 0.6;      // 지속 중 조각 흐름 속도 (칸/틱 = 8칸/초)

    private final ItemRenderer itemRenderer;
    private ItemStack beamStack = ItemStack.EMPTY;   // 빔 조각 아이템 (한 번만 생성)

    public LaserBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public boolean shouldRender(LaserBeamEntity entity, Frustum frustum, double x, double y, double z) {
        return true;   // 빔이 엔티티 박스보다 훨씬 길어서 화면 컬링 끔
    }

    @Override
    public void render(LaserBeamEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Player owner = entity.getOwnerUUID()
                .map(uuid -> entity.level().getPlayerByUUID(uuid))
                .orElse(null);
        if (owner == null) {
            return;
        }

        // 매 프레임 발사자 시선 기준으로 빔 경로 계산 (조준을 부드럽게 따라감)
        // 시작점은 눈이 아니라 몸(가슴)에서
        Vec3 eye = owner.getEyePosition(partialTicks).subtract(0.0D, BODY_OFFSET, 0.0D);
        Vec3 look = owner.getViewVector(partialTicks);
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult hit = entity.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 tip = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        double len = eye.distanceTo(tip);

        poseStack.pushPose();
        // 원점(엔티티 위치) → 발사자 눈 위치로
        Vec3 entityPos = entity.getPosition(partialTicks);
        poseStack.translate(eye.x - entityPos.x, eye.y - entityPos.y, eye.z - entityPos.z);
        // 시선 방향으로 회전 (이후 +Z = 시선 방향)
        poseStack.mulPose(Axis.YP.rotationDegrees(-owner.getViewYRot(partialTicks)));
        poseStack.mulPose(Axis.XP.rotationDegrees(owner.getViewXRot(partialTicks)));
        poseStack.scale(BEAM_SCALE, BEAM_SCALE, 1.0F);   // 굵기(X·Y)만 확대, 길이(Z)는 유지

        float age = entity.tickCount + partialTicks;   // 빔이 켜진 뒤 지난 시간(틱)

        // ① 발사 순간: 끝이 앞으로 전진하며 뻗어나감 (0.5초쯤에 벽 도달)
        double visLen = Math.min(len, age * GROW_SPEED);

        // ② 지속 중: 조각들이 몸→벽 방향으로 계속 흐름 (뒤에서 새 조각이 나오는 느낌)
        double phase = (age * FLOW_SPEED) % 1.0;

        // 1칸짜리 빔 조각을 흐름 위상만큼 밀면서 이어 그리기 (아이템 객체는 최초 1회만 생성)
        if (this.beamStack.isEmpty()) {
            this.beamStack = new ItemStack(ModItems.LASER.get());
        }
        ItemStack beam = this.beamStack;
        int count = (int) Math.ceil(visLen + 1.0);
        for (int i = 0; i < count; i++) {
            double z = phase - 0.5 + i;   // 조각 중심 (앞뒤 반 칸씩 차지)
            if (z - 0.5 > visLen) {
                break;   // 빔 끝을 넘어가면 중단
            }
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, z);
            this.itemRenderer.renderStatic(beam, ItemDisplayContext.GROUND,
                    FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            poseStack.popPose();
        }
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LaserBeamEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;   // 아이템 렌더라 실제로 안 쓰임
    }
}
