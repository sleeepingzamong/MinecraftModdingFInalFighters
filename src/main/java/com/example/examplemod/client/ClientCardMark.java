package com.example.examplemod.client;

// 갬블러 본인 클라이언트가 기억하는 블랙잭 카드 상태 (CardMarkS2CPacket이 갱신)
public class ClientCardMark {

    private static int targetId = -1;
    private static int sum = 0;
    private static int ticks = 0;

    public static void set(int newTargetId, int newSum, int newTicks) {
        if (newTicks <= 0 || newTargetId == -1) {
            targetId = -1;
            ticks = 0;
        } else {
            targetId = newTargetId;
            sum = newSum;
            ticks = newTicks;
        }
    }

    public static boolean isMarked(int entityId) {
        return ticks > 0 && entityId == targetId;
    }

    public static int getSum() {
        return sum;
    }

    // 서버 해제 패킷이 유실돼도 저절로 꺼지게 카운트다운
    public static void tickDown() {
        if (ticks > 0) {
            ticks--;
        }
    }

    public static void clearAll() {
        targetId = -1;
        ticks = 0;
    }
}
