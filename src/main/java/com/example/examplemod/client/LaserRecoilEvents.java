package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 레이저 궁극기 반동: 충전(약하게)·발사(강하게) 동안 조준점 + 화면 흔들림
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class LaserRecoilEvents {

    private static final float CHARGE_AIM_SHAKE = 0.4F;   // 충전 중 조준 흔들림 (도/틱)
    private static final float FIRE_AIM_SHAKE = 0.8F;     // 발사 중 조준 흔들림
    private static final float CHARGE_CAM_SHAKE = 0.3F;   // 충전 중 화면 떨림 (도/프레임)
    private static final float FIRE_CAM_SHAKE = 0.1F;     // 발사 중 화면 떨림

    private static final RandomSource RANDOM = RandomSource.create();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ClientCombatData.tickLaserState();

        // 조준점 자체를 흔듦 → 빔·데미지 방향도 같이 흔들림 (진짜 반동)
        float shake = aimShake();
        if (shake > 0.0F) {
            player.setYRot(player.getYRot() + (RANDOM.nextFloat() - 0.5F) * 2.0F * shake);
            player.setXRot(Mth.clamp(
                    player.getXRot() + (RANDOM.nextFloat() - 0.5F) * 2.0F * shake, -90.0F, 90.0F));
        }
    }

    // 카메라만 흔듦 (조준 무관, 시각 연출) — roll이 반동 느낌을 살림
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        float shake = camShake();
        if (shake > 0.0F) {
            event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5F) * 2.0F * shake);
            event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5F) * 2.0F * shake);
            event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5F) * 2.0F * shake);
        }
    }

    private static float aimShake() {
        if (ClientCombatData.isLaserCharging()) {
            return CHARGE_AIM_SHAKE;
        }
        if (ClientCombatData.isLaserFiring()) {
            return FIRE_AIM_SHAKE;
        }
        return 0.0F;
    }

    private static float camShake() {
        if (ClientCombatData.isLaserCharging()) {
            return CHARGE_CAM_SHAKE;
        }
        if (ClientCombatData.isLaserFiring()) {
            return FIRE_CAM_SHAKE;
        }
        return 0.0F;
    }
}
