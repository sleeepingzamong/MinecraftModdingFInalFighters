package com.example.examplemod.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// 캐릭터 전용 장비의 커스텀 3D 모델 — 바닐라 갑옷처럼 몸을 통째로 부풀리지 않고,
// 장신구/방어구의 실제 형태(얇은 머리테, 챙 달린 모자, 렌즈, 뿔, 후드 등)를 조각한다.
// 텍스처는 64x64, UV는 아래 각 빌더의 texOffs가 결정한다.
@OnlyIn(Dist.CLIENT)
public class CharacterArmorModel extends HumanoidModel<LivingEntity> {

    // 후드집업처럼 "가슴 장비인데 머리 파츠(후드)를 쓰는" 모델은 몸이 보일 때 머리도 같이 그림
    private final boolean headFollowsBody;

    public CharacterArmorModel(ModelPart root, boolean headFollowsBody) {
        super(root);
        this.headFollowsBody = headFollowsBody;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        if (headFollowsBody) {
            this.head.visible = this.body.visible;
            this.hat.visible = false;
        }
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ==================== 지오메트리 ====================

    public static LayerDefinition createLayer(String name) {
        MeshDefinition mesh = emptyMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        PartDefinition body = root.getChild("body");
        PartDefinition rightArm = root.getChild("right_arm");
        PartDefinition leftArm = root.getChild("left_arm");

        switch (name) {
            case "monkey" -> {
                // 긴고아: 머리에 밀착된 두께 1px 금 테 + 이마 앞 소용돌이 장식 2개
                // (치수는 정수 9 + 축소 변형 — 소수점 치수는 UV가 모자라 뒷면에 구멍이 남)
                head.addOrReplaceChild("ring", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -7.4F, -4.5F, 9, 1, 9,
                                new CubeDeformation(-0.35F, 0.0F, -0.35F)), PartPose.ZERO);
                head.addOrReplaceChild("curl_l", CubeListBuilder.create()
                        .texOffs(34, 0).addBox(-2.1F, -8.3F, -4.55F, 2, 2, 1), PartPose.ZERO);
                head.addOrReplaceChild("curl_r", CubeListBuilder.create().mirror()
                        .texOffs(34, 0).addBox(0.1F, -8.3F, -4.55F, 2, 2, 1), PartPose.ZERO);
            }
            case "gambler" -> {
                // 중절모: 높은 크라운이 머리를 감싸고(9.2 > 8) 얇고 넓은 챙이 이마 위에
                head.addOrReplaceChild("brim", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.5F, -6.0F, -6.5F, 13, 1, 13), PartPose.ZERO);
                head.addOrReplaceChild("crown", CubeListBuilder.create()
                        .texOffs(0, 27).addBox(-4.5F, -11.0F, -4.5F, 9, 5, 9,
                                new CubeDeformation(0.1F, 0.2F, 0.1F)), PartPose.ZERO);
            }
            case "swordsman" -> {
                // 하치마키(각오의 머리띠): 흰 천 띠 + 이마 붉은 원 + 뒤통수 매듭 + 늘어지는 끈 2가닥
                head.addOrReplaceChild("band", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -7.7F, -4.5F, 9, 2, 9,
                                new CubeDeformation(-0.2F, 0.0F, -0.2F)), PartPose.ZERO);
                head.addOrReplaceChild("knot", CubeListBuilder.create()
                        .texOffs(34, 0).addBox(-1.0F, -7.4F, 4.1F, 2, 2, 1), PartPose.ZERO);
                head.addOrReplaceChild("tail_l", CubeListBuilder.create()
                                .texOffs(40, 0).addBox(-0.5F, 0.0F, 0.0F, 1, 5, 0),
                        PartPose.offsetAndRotation(0.8F, -6.4F, 4.8F, 0.25F, 0.0F, 0.35F));
                head.addOrReplaceChild("tail_r", CubeListBuilder.create().mirror()
                                .texOffs(40, 0).addBox(-0.5F, 0.0F, 0.0F, 1, 5, 0),
                        PartPose.offsetAndRotation(-0.8F, -6.4F, 4.8F, 0.25F, 0.0F, -0.35F));
            }
            case "hacker" -> {
                // 글리치 후드집업 (몸 장비): 밀착 재킷 + 소매 + 머리에 쓴 후드 — 네온 회로
                body.addOrReplaceChild("jacket", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.35F)), PartPose.ZERO);
                rightArm.addOrReplaceChild("sleeve_r", CubeListBuilder.create()
                        .texOffs(26, 0).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.3F)), PartPose.ZERO);
                leftArm.addOrReplaceChild("sleeve_l", CubeListBuilder.create().mirror()
                        .texOffs(26, 0).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.3F)), PartPose.ZERO);
                head.addOrReplaceChild("hood", CubeListBuilder.create()
                        .texOffs(0, 34).addBox(-4.5F, -8.8F, -4.4F, 9, 9, 9, new CubeDeformation(0.2F)), PartPose.ZERO);
            }
            case "hammer" -> {
                // 가면: 얼굴에 밀착된 판 (눈구멍은 텍스처 투명으로 뚫림) + 옆 끈
                head.addOrReplaceChild("mask", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -8.6F, -5.1F, 9, 10, 1), PartPose.ZERO);
                head.addOrReplaceChild("strap", CubeListBuilder.create()
                        .texOffs(22, 0).addBox(-4.4F, -6.5F, -4.4F, 9, 1, 9), PartPose.ZERO);
            }
            case "cyborg" -> {
                // 외골격 흉갑: 몸에 밀착(+0.35) + 튀어나온 발광 코어 + 어깨 서보 캡
                body.addOrReplaceChild("chest", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.35F)), PartPose.ZERO);
                body.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-1.0F, 2.0F, -3.0F, 2, 4, 1), PartPose.ZERO);
                rightArm.addOrReplaceChild("pauldron_r", CubeListBuilder.create()
                        .texOffs(26, 0).addBox(-3.6F, -2.5F, -2.5F, 5, 4, 5), PartPose.ZERO);
                leftArm.addOrReplaceChild("pauldron_l", CubeListBuilder.create().mirror()
                        .texOffs(26, 0).addBox(-1.4F, -2.5F, -2.5F, 5, 4, 5), PartPose.ZERO);
                // 팔 전체 강철 소매 — 글러브까지 내려와 팔 스킨이 안 보임
                rightArm.addOrReplaceChild("sleeve_r", CubeListBuilder.create()
                        .texOffs(26, 10).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.25F)), PartPose.ZERO);
                leftArm.addOrReplaceChild("sleeve_l", CubeListBuilder.create().mirror()
                        .texOffs(26, 10).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.25F)), PartPose.ZERO);
            }
            case "berserker" -> {
                // 늑대 모피: 상체 모피 숄 + 벨트 + 목 뒤 갈기 + 두툼한 어깨 모피
                body.addOrReplaceChild("shawl", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8, 7, 4, new CubeDeformation(0.4F)), PartPose.ZERO);
                body.addOrReplaceChild("belt", CubeListBuilder.create()
                        .texOffs(0, 19).addBox(-4.0F, 7.0F, -2.0F, 8, 2, 4, new CubeDeformation(0.45F)), PartPose.ZERO);
                body.addOrReplaceChild("mane", CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-3.0F, -1.5F, 1.6F, 6, 4, 2), PartPose.ZERO);
                rightArm.addOrReplaceChild("fur_r", CubeListBuilder.create()
                        .texOffs(26, 0).addBox(-3.7F, -2.7F, -2.7F, 5, 4, 5, new CubeDeformation(0.2F)), PartPose.ZERO);
                leftArm.addOrReplaceChild("fur_l", CubeListBuilder.create().mirror()
                        .texOffs(26, 0).addBox(-1.3F, -2.7F, -2.7F, 5, 4, 5, new CubeDeformation(0.2F)), PartPose.ZERO);
            }
            case "phoenix" -> {
                // 불사조 날개 한 쌍 (두께 0.3): 어깨뼈에서 뻗는 날개팔(덮깃) + 첫째깃 5장 + 둘째깃 3장 — 몸은 안 덮음
                phoenixWing(body, true);
                phoenixWing(body, false);
            }
            case "dojuk" -> {
                // 누더기 검은 망토: 몸에 밀착해 흘러내리는 천 + 얇은 어깨 덮개 + 입까지 가리는 깊은 후드
                body.addOrReplaceChild("mantle", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8, 8, 4, new CubeDeformation(0.25F)), PartPose.ZERO);
                // 어깨: 팔에 딱 붙는 반소매 (위로 안 튀어나오고 팔 아래로 내려옴, 끝단 너덜)
                rightArm.addOrReplaceChild("shoulder_r", CubeListBuilder.create()
                        .texOffs(26, 0).addBox(-3.0F, -2.0F, -2.0F, 4, 5, 4, new CubeDeformation(0.25F)), PartPose.ZERO);
                leftArm.addOrReplaceChild("shoulder_l", CubeListBuilder.create().mirror()
                        .texOffs(26, 0).addBox(-1.0F, -2.0F, -2.0F, 4, 5, 4, new CubeDeformation(0.25F)), PartPose.ZERO);
                head.addOrReplaceChild("hood", CubeListBuilder.create()
                        .texOffs(0, 34).addBox(-4.5F, -8.8F, -4.4F, 9, 9, 9, new CubeDeformation(0.25F)), PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    // ==================== 불사조 날개 ====================

    // 두께 1 박스를 z로 0.35씩 줄여 0.3 — 소수 치수는 UV가 어긋나므로 정수 치수 + 축소 변형
    private static final CubeDeformation WING_THIN = new CubeDeformation(0.0F, 0.0F, -0.35F);

    // 왼쪽 날개 기준 깃 표 (오른쪽은 x·각도 반전 + 텍스처 미러). 피벗은 날개팔 중심선(30도) 위의 점.
    // 층z: 이웃한 깃끼리만 다른 층(0 / 0.35)이면 z파이팅 없음 — 층을 적게 쌓아야 날개가 등에 붙어 보임.
    // 날개팔은 위층(0.7)에서 깃 뿌리를 덮고, 맨 위 첫째깃은 10도 깃과 겹쳐서 몸쪽 층(-0.35)
    // {texU, texV, 길이, 폭, 시작x, 피벗x, 피벗y, 층z, 각도(도, 위쪽 +)}
    private static final float[][] PHOENIX_WING = {
            {32, 10, 6, 3, 0, 1.169F, -0.675F, 0.0F, -86},   // 둘째깃 (몸쪽부터, 아래로 늘어짐)
            {32, 5, 6, 3, 0, 2.338F, -1.35F, 0.35F, -68},
            {32, 0, 7, 3, 0, 3.464F, -2.0F, 0.0F, -50},
            {0, 26, 8, 3, 0, 4.677F, -2.7F, 0.35F, -30},     // 첫째깃 (손 부분을 따라 아래→위)
            {0, 21, 9, 3, 0, 5.456F, -3.15F, 0.0F, -10},
            {0, 16, 10, 3, 0, 6.235F, -3.6F, 0.35F, 10},
            {0, 11, 10, 3, 0, 7.015F, -4.05F, 0.0F, 30},
            {0, 6, 9, 3, 0, 7.794F, -4.5F, -0.35F, 50},
            {0, 0, 12, 4, -1, 0, 0, 0.7F, 30},               // 날개팔 (덮깃)
    };

    private static void phoenixWing(PartDefinition body, boolean left) {
        float side = left ? 1.0F : -1.0F;
        // 뿌리 = 어깨뼈. z 2.63 = 가장 안쪽 둘째깃 끝이 등을 뚫지 않는 가장 가까운 위치
        // 윗부분은 살짝 뒤로 젖히고(xRot) 바깥쪽은 뒤로 꺾음(yRot)
        PartDefinition wing = body.addOrReplaceChild(left ? "wing_l" : "wing_r", CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.0F * side, 3.5F, 2.63F, -0.12F, -0.35F * side, 0.0F));
        for (int i = 0; i < PHOENIX_WING.length; i++) {
            float[] f = PHOENIX_WING[i];
            int length = (int) f[2];
            int width = (int) f[3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs((int) f[0], (int) f[1]);
            // z -0.35에서 시작하는 두께 1 박스 → 축소 후 두께가 층 기준 [0, 0.3]에 붙음 (칸 가운데에 뜨지 않게)
            if (left) {
                cube.addBox(f[4], -width / 2.0F, -0.35F, length, width, 1, WING_THIN);
            } else {
                cube.mirror().addBox(-f[4] - length, -width / 2.0F, -0.35F, length, width, 1, WING_THIN);
            }
            wing.addOrReplaceChild("feather_" + i, cube, PartPose.offsetAndRotation(
                    f[5] * side, f[6], f[7], 0.0F, 0.0F, (float) Math.toRadians(-f[8]) * side));
        }
    }

    // HumanoidModel이 요구하는 파츠 전부를 빈 상태로 준비
    private static MeshDefinition emptyMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return mesh;
    }
}
