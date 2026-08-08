package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.combat.SkillType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;

// 클라이언트가 기억하는 "내 스킬 쿨타임" (CooldownS2CPacket이 시작을 알려주면 매 틱 카운트다운)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class ClientSkillCooldowns {

    // [0] = 남은 틱, [1] = 전체 틱 (HUD의 덮개 비율 계산용)
    private static final Map<SkillType, int[]> COOLDOWNS = new EnumMap<>(SkillType.class);

    public static void set(SkillType slot, int ticks) {
        COOLDOWNS.put(slot, new int[]{ticks, ticks});
    }

    public static int getRemaining(SkillType slot) {
        int[] v = COOLDOWNS.get(slot);
        return v == null ? 0 : v[0];
    }

    public static int getTotal(SkillType slot) {
        int[] v = COOLDOWNS.get(slot);
        return v == null ? 0 : v[1];
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (int[] v : COOLDOWNS.values()) {
            if (v[0] > 0) {
                v[0]--;
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        COOLDOWNS.clear();
    }
}
