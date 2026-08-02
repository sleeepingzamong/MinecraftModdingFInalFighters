package com.example.examplemod.client;

import java.util.HashMap;
import java.util.Map;

// 클라이언트가 기억하는 "플레이어별 패링 표식 개수" (ParryMarksS2CPacket이 갱신)
public class ClientParryMarks {

    private static final Map<Integer, Integer> MARKS = new HashMap<>();

    public static void set(int entityId, int marks) {
        if (marks <= 0) {
            MARKS.remove(entityId);
        } else {
            MARKS.put(entityId, marks);
        }
    }

    public static int get(int entityId) {
        return MARKS.getOrDefault(entityId, 0);
    }

    public static void clearAll() {
        MARKS.clear();
    }
}
