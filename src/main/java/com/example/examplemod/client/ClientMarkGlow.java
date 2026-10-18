package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// 도적 표식 대상 발광 — 시전자 클라이언트에서만 setGlowingTag로 외곽선 표시
// (서버가 MarkGlowS2CPacket으로 켜고 끔; 만료 시간은 안전장치로 클라에서도 카운트다운)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ClientMarkGlow {

    private static final Map<Integer, Integer> GLOW_TICKS = new HashMap<>();

    public static void set(int entityId, int ticks) {
        if (ticks <= 0) {
            GLOW_TICKS.remove(entityId);
            applyGlow(entityId, false);
        } else {
            GLOW_TICKS.put(entityId, ticks + 10);   // 서버 해제 패킷이 유실돼도 곧 꺼지게 여유 10틱
            applyGlow(entityId, true);
        }
    }

    private static void applyGlow(int entityId, boolean glow) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            entity.setGlowingTag(glow);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || GLOW_TICKS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, Integer>> it = GLOW_TICKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            if (entry.getValue() <= 1) {
                applyGlow(entry.getKey(), false);
                it.remove();
            } else {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }
}
