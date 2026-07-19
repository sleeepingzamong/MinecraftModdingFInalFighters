package com.example.examplemod.client;

import net.minecraft.client.model.HumanoidModel;

// 해머 회전 베기 전용 팔 자세 (Forge 커스텀 ArmPose — 게임의 자세 계산 안에서 적용돼 덮어쓰기 안 당함)
// 주의: 반드시 클라 셋업(onClientSetup)에서 init()으로 미리 생성해야 함.
// 첫 렌더링 이후에 만들면 바닐라 switch 내부 테이블(10칸)이 이미 굳어서 ArrayIndexOutOfBounds 크래시.
public class HammerPoses {

    // 클래스 로드(=SPIN 생성)를 게임 시작 시점으로 앞당기기 위한 트리거
    public static void init() {
    }

    // 양팔을 앞으로 쭉 뻗고 손이 가운데로 모이는 자세 (해머 던지기 선수처럼)
    // 회전 중엔 웅크림(허리 굽힘)을 강제하는데, 바닐라 웅크림 처리가 이 "이후" 팔에 +0.4을 더하므로
    // 여기서 -1.85로 잡아 최종적으로 약 -1.45(수평보다 살짝 위)가 되게 보정
    public static final HumanoidModel.ArmPose SPIN = HumanoidModel.ArmPose.create(
            "EXAMPLEMOD_HAMMER_SPIN", true, (model, entity, arm) -> {
                model.rightArm.xRot = -1.85F;
                model.leftArm.xRot = -1.85F;
                model.rightArm.yRot = -0.35F;   // 손이 가운데로 모이게
                model.leftArm.yRot = 0.35F;
                model.rightArm.zRot = 0F;
                model.leftArm.zRot = 0F;
            });
}
