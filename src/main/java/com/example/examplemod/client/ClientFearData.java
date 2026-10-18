package com.example.examplemod.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// 클라이언트가 기억하는 "플레이어별 공포 남은 틱" (FearS2CPacket이 갱신, 매 틱 감소)
public class ClientFearData {

    private static final Map<Integer, Integer> FEAR_TICKS = new HashMap<>();

    public static void set(int entityId, int ticks) {
        if (ticks <= 0) {
            FEAR_TICKS.remove(entityId);
        } else {
            FEAR_TICKS.put(entityId, ticks);
        }
    }

    public static int get(int entityId) {
        return FEAR_TICKS.getOrDefault(entityId, 0);
    }

    // FearRenderEvents가 클라 틱마다 호출 — 남은 시간 카운트다운
    public static void tickDown() {
        Iterator<Map.Entry<Integer, Integer>> it = FEAR_TICKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            if (entry.getValue() <= 1) {
                it.remove();
            } else {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    public static void clearAll() {
        FEAR_TICKS.clear();
    }
}
