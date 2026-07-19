package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

// 해머 회전 베기 시각 처리:
//  - 회전 중인 플레이어의 "그려지는 모델"만 Y축 회전 + 뒤로 젖힘 (실제 방향/카메라는 그대로)
//  - 걷기 애니메이션 정지 (양발 곧게)
//  - 본인이면 회전 내내 3인칭 유지 (F5 눌러도 되돌림)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class HammerSpinRenderEvents {

    private static final float LEAN_BACK_DEGREES = 15F;   // 중심이 뒤로 젖혀지는 각도

    // 렌더링 동안만 웅크림 자세로 바꿔 허리를 굽힘 (Post에서 원상복구 → 게임플레이·히트박스 영향 없음)
    // 바닐라가 웅크림일 때 body.xRot=0.5 + 부위 오프셋을 적용해줌 — 이게 곧 "허리 굽힘" 자세
    // 지연 초기화인 이유: 핫스왑으로 새 static 필드가 추가되면 초기화가 안 돼 null로 남음 (2026-07-18 크래시)
    private static Map<Integer, Pose> savedPoses;

    private static Map<Integer, Pose> savedPoses() {
        if (savedPoses == null) {
            savedPoses = new HashMap<>();
        }
        return savedPoses;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!ClientSpinState.isSpinning(player)) {
            return;
        }

        savedPoses().put(player.getId(), player.getPose());
        player.setPose(Pose.CROUCHING);   // 허리 굽힘 (렌더링 한정)

        float angle = ClientSpinState.getSpinAngle(player, event.getPartialTick());
        PoseStack pose = event.getPoseStack();
        pose.mulPose(Axis.YP.rotationDegrees(angle));            // 빙글빙글
        pose.mulPose(Axis.XP.rotationDegrees(LEAN_BACK_DEGREES)); // 회전과 함께 도는 뒤로 젖힘
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Pose saved = savedPoses().remove(player.getId());
        if (saved != null) {
            player.setPose(saved);   // 렌더 끝 → 실제 자세 복원
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ClientSpinState.clearAll();   // 월드 나감 → 상태 초기화
            return;
        }
        if (!ClientSpinState.hasAny()) {
            return;
        }

        // 회전 중인 플레이어는 걷기 애니메이션 정지 → 다리가 곧게 펴진 채 회전
        for (Player p : mc.level.players()) {
            if (ClientSpinState.isSpinning(p)) {
                p.walkAnimation.setSpeed(0F);
            }
        }

        // 본인: 회전 내내 3인칭 강제 (중간에 F5 눌러도 되돌림)
        if (mc.player != null && ClientSpinState.isSpinning(mc.player)
                && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }
}
