package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 가드 연출 상태: 가드 중 파란 비네트 + 크로스헤어 방패 아이콘, 막아낼 때 플래시·펄스
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ClientGuardFx {

    public static final int BLOCK_FLASH_MAX = 12;   // 막아낸 순간 플래시 지속 (틱)

    private static int blockFlashTicks = 0;
    private static int blockFlashTicksO = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        blockFlashTicksO = blockFlashTicks;
        if (blockFlashTicks > 0) {
            blockFlashTicks--;
        }
    }

    // GuardBlockS2CPacket이 호출 — 가드로 피해를 막아냄
    public static void onBlock() {
        blockFlashTicks = BLOCK_FLASH_MAX;
    }

    // 가드 중 확대(줌) 억제: 이속 감소가 FOV를 0.55배까지 조이는 것을 0.9배로 클램프
    @SubscribeEvent
    public static void onComputeFov(net.minecraftforge.client.event.ComputeFovModifierEvent event) {
        if (ClientSkillKeyEvents.isGuardingNow() && event.getNewFovModifier() < 0.9f) {
            event.setNewFovModifier(0.9f);
        }
    }

    // 비네트 알파: 가드 중 은은한 기본값 + 막아낸 순간 플래시
    public static float vignetteAlpha(float partialTick) {
        float base = ClientSkillKeyEvents.isGuardingNow() ? 0.24f : 0f;
        float flash = (blockFlashTicksO + (blockFlashTicks - blockFlashTicksO) * partialTick)
                / BLOCK_FLASH_MAX * 0.35f;
        float total = base + Math.max(0f, flash);
        return Math.min(0.6f, total);
    }

    // 방패 아이콘 펄스 진행도 (0 = 평상시, 1 = 막아낸 직후)
    public static float iconPulse(float partialTick) {
        return Math.max(0f, (blockFlashTicksO + (blockFlashTicks - blockFlashTicksO) * partialTick)
                / BLOCK_FLASH_MAX);
    }
}
