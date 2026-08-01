package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

    // 자세(허리·팔·다리)는 playerAnimator 모션(hammer_spin.json)이 담당.
    // 여기서는 모델 전체를 Y축으로 돌리는 "회전"만 처리한다.
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!ClientSpinState.isSpinning(player)) {
            return;
        }

        float angle = ClientSpinState.getSpinAngle(player, event.getPartialTick());
        PoseStack pose = event.getPoseStack();
        pose.mulPose(Axis.YP.rotationDegrees(angle));   // 빙글빙글
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
