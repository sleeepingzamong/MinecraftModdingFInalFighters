package com.example.examplemod.client;

import com.example.examplemod.entity.CloneEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// 분신 전용 모델: 바닐라 플레이어 모델 + 복싱 가드 자세
//  - 평소: 양팔을 앞으로 들어 가드 (숨쉬듯 살짝 흔들림 — 살아있는 느낌)
//  - 공격 스윙 중: 바닐라 팔 휘두르기가 그대로 재생됨
@OnlyIn(Dist.CLIENT)
public class CloneModel extends PlayerModel<CloneEntity> {

    public CloneModel(ModelPart root) {
        super(root, false);   // false = 넓은 팔(WIDE)
    }

    @Override
    public void setupAnim(CloneEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 공격 스윙 중엔 바닐라 휘두르기를 살리고, 아닐 때만 가드 자세로 덮어씀
        if (this.attackTime <= 0.0F) {
            float breathe = Mth.sin(ageInTicks * 0.15F) * 0.06F;   // 숨쉬기 흔들림
            float walkDamp = 1.0F - Mth.clamp(limbSwingAmount * 1.5F, 0.0F, 0.55F);   // 걸을 땐 가드 살짝 풀림

            this.rightArm.xRot = (-1.15F + breathe) * walkDamp
                    + this.rightArm.xRot * (1.0F - walkDamp) * 0.4F;
            this.rightArm.yRot = -0.35F * walkDamp;
            this.rightArm.zRot = 0.08F;

            this.leftArm.xRot = (-1.15F - breathe) * walkDamp
                    + this.leftArm.xRot * (1.0F - walkDamp) * 0.4F;
            this.leftArm.yRot = 0.35F * walkDamp;
            this.leftArm.zRot = -0.08F;
        }

        // PlayerModel은 super.setupAnim 끝에서 소매를 복사하므로, 우리가 팔을 고친 뒤 다시 맞춰줌
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
    }
}
