package com.example.examplemod.client;

// 클라이언트가 기억하는 "내 분노 스택" (RageS2CPacket이 갱신, 광전사 HUD 표시용)
public class ClientRageData {

    private static int stacks = 0;

    public static void set(int value) {
        stacks = value;
    }

    public static int get() {
        return stacks;
    }

    public static void clear() {
        stacks = 0;
    }
}
