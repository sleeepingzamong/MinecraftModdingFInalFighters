package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 빈사 상태 (체력 6 이하) 클라이언트 연출: 심장박동 소리 + 화면 비네트 펄스
// 자기 체력은 클라이언트가 이미 알고 있어서 패킷 없이 동작한다. 회복하면 즉시 멈춤.
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class LowHealthEffects {

    public static final float THRESHOLD = 6.0F;      // 빈사 기준 체력 (서버 LowHealthEvents와 동일)
    private static final int BEAT_SLOW = 20;         // 체력 4~6: 1초 간격
    private static final int BEAT_FAST = 14;         // 체력 3 이하: 0.7초 간격

    private static int beatTimer = 0;
    private static float pulse = 0f;                 // 박동 순간 1 → 서서히 0 (비네트 펄스)
    private static float pulseO = 0f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        pulseO = pulse;
        pulse = Math.max(0f, pulse - 0.10f);

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused() || !player.isAlive()
                || player.isSpectator() || player.isCreative()
                || player.getHealth() > THRESHOLD) {
            beatTimer = 0;
            return;
        }

        if (--beatTimer <= 0) {
            beatTimer = player.getHealth() <= 3.0F ? BEAT_FAST : BEAT_SLOW;
            pulse = 1.0f;
            player.playSound(SoundEvents.WARDEN_HEARTBEAT, 0.9F, 1.0F);   // 본인에게만 들림
        }
    }

    // 비네트 오버레이 알파 (0 = 표시 안 함). 기본 진하기는 체력 비례 + 박동 펄스
    public static float vignetteAlpha(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive() || player.isSpectator()
                || player.isCreative() || player.getHealth() > THRESHOLD) {
            return 0f;
        }
        float base = 0.18f + (THRESHOLD - player.getHealth()) / THRESHOLD * 0.30f;
        float p = pulseO + (pulse - pulseO) * partialTick;
        return Math.min(0.75f, base + p * 0.22f);
    }
}
