package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 스태미나·궁 게이지 바의 연출 상태 (소모 잔상, 획득 반짝, 부족 흔들림, 궁 완충 연출)
// CombatHudOverlay가 매 프레임 읽어서 그린다.
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class HudFx {

    public static final int CONSUME_FLASH_MAX = 8;   // 소모 잔상 지속 (틱)
    public static final int GAIN_FLASH_MAX = 6;      // 회복 칸 반짝 지속
    public static final int SHAKE_MAX = 10;          // 부족 흔들림 지속
    public static final int ULT_FLASH_MAX = 24;      // 궁 완충 플래시 지속

    private static int prevStamina = -1;
    private static int prevGauge = -1;

    static int consumeFlashTicks = 0;   // 스태미나 소모 잔상 남은 틱
    static int consumeFrom = 0;         // 소모 직전 값 (잔상이 덮는 구간: to..from-1 칸)
    static int consumeTo = 0;
    static int gainCell = -1;           // 방금 회복된 칸 번호
    static int gainFlashTicks = 0;
    static int staminaShakeTicks = 0;   // 스태미나 부족 흔들림
    static int ultShakeTicks = 0;       // 궁 게이지 부족 흔들림
    static int ultFlashTicks = 0;       // 궁 100% 도달 플래시

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (consumeFlashTicks > 0) consumeFlashTicks--;
        if (gainFlashTicks > 0) gainFlashTicks--;
        if (staminaShakeTicks > 0) staminaShakeTicks--;
        if (ultShakeTicks > 0) ultShakeTicks--;
        if (ultFlashTicks > 0) ultFlashTicks--;

        int st = ClientCombatData.getStamina();
        if (prevStamina >= 0 && st != prevStamina) {
            if (st < prevStamina) {
                consumeFlashTicks = CONSUME_FLASH_MAX;   // 줄어든 칸에 하얀 잔상
                consumeFrom = prevStamina;
                consumeTo = st;
            } else {
                gainCell = st - 1;                       // 방금 찬 칸 반짝
                gainFlashTicks = GAIN_FLASH_MAX;
            }
        }
        prevStamina = st;

        int gauge = ClientCombatData.getUltimateGauge();
        if (prevGauge >= 0 && gauge >= 100 && prevGauge < 100) {
            ultFlashTicks = ULT_FLASH_MAX;               // 완충: 플래시 + "띠링"
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(SoundEvents.PLAYER_LEVELUP, 0.55F, 1.6F);
            }
        }
        prevGauge = gauge;
    }

    // 스태미나 없이 스킬·대쉬를 누름 → 바 빨간 흔들림 (ClientSkillKeyEvents가 호출)
    public static void triggerLowStamina() {
        staminaShakeTicks = SHAKE_MAX;
    }

    // 게이지가 안 찼는데 궁을 누름 → 궁 바 빨간 흔들림
    public static void triggerUltNotReady() {
        ultShakeTicks = SHAKE_MAX;
    }

    // 흔들림 X 오프셋 (프레임용)
    static int shakeOffset(int ticksLeft) {
        if (ticksLeft <= 0) {
            return 0;
        }
        return (int) Math.round(Math.sin(ticksLeft * 2.1) * 2.0);
    }
}
