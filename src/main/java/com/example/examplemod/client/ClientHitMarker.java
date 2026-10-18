package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 히트마커 상태: 적중 시 조준점 주위 X자 4획, 처치 시 빨갛고 크게 (CombatHudOverlay가 그림)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ClientHitMarker {

    public static final int HIT_TICKS = 7;     // 일반 적중 표시 시간
    public static final int KILL_TICKS = 16;   // 처치 표시 시간

    private static int ticksLeft = 0;
    private static int ticksLeftO = 0;
    private static boolean kill = false;

    public static void onHit(boolean isKill) {
        if (isKill) {
            kill = true;
            ticksLeft = KILL_TICKS;
        } else if (ticksLeft <= 0 || !kill) {
            kill = false;
            ticksLeft = HIT_TICKS;   // 킬 마커 표시 중엔 일반 마커가 덮지 않음
        }
        ticksLeftO = ticksLeft;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ticksLeftO = ticksLeft;
        if (ticksLeft > 0) {
            ticksLeft--;
        }
    }

    public static boolean isKill() {
        return kill;
    }

    // 남은 진행도 0~1 (1 = 방금 적중)
    public static float progress(float partialTick) {
        float max = kill ? KILL_TICKS : HIT_TICKS;
        float cur = ticksLeftO + (ticksLeft - ticksLeftO) * partialTick;
        return Math.max(0f, cur / max);
    }
}
