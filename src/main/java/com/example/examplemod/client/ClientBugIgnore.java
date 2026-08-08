package com.example.examplemod.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// 클라이언트가 기억하는 "플레이어별 버그 무시 남은 틱" (BugIgnoreS2CPacket이 갱신)
public class ClientBugIgnore {

    private static final Map<Integer, Integer> TICKS = new HashMap<>();

    public static void set(int entityId, int ticks) {
        if (ticks <= 0) {
            TICKS.remove(entityId);
        } else {
            TICKS.put(entityId, ticks);
        }
    }

    public static int get(int entityId) {
        return TICKS.getOrDefault(entityId, 0);
    }

    // 매 클라 틱마다 카운트다운
    public static void tickAll() {
        Iterator<Map.Entry<Integer, Integer>> it = TICKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> e = it.next();
            if (e.getValue() <= 1) {
                it.remove();
            } else {
                e.setValue(e.getValue() - 1);
            }
        }
    }

    public static void clearAll() {
        TICKS.clear();
    }
}
