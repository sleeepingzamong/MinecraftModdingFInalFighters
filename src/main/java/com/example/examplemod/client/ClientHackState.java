package com.example.examplemod.client;

// 클라이언트가 기억하는 "내가 해킹당한 남은 시간" (SystemHackS2CPacket이 갱신)
public class ClientHackState {

    private static int ticksLeft = 0;

    public static void set(int ticks) {
        ticksLeft = ticks;
    }

    public static boolean isHacked() {
        return ticksLeft > 0;
    }

    public static int getTicksLeft() {
        return ticksLeft;
    }

    public static void tickDown() {
        if (ticksLeft > 0) {
            ticksLeft--;
        }
    }
}
