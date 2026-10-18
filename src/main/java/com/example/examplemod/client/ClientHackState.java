package com.example.examplemod.client;

// 클라이언트가 기억하는 "내가 해킹당한 남은 시간" (SystemHackS2CPacket이 갱신)
public class ClientHackState {

    private static int ticksLeft = 0;
    private static net.minecraft.client.resources.sounds.SimpleSoundInstance ambience = null;

    public static void set(int ticks) {
        ticksLeft = ticks;
        // 해킹 지속음: 걸리면 본인에게만 13초 재생, 조기 해제(사망 등)면 즉시 중단
        var soundManager = net.minecraft.client.Minecraft.getInstance().getSoundManager();
        if (ambience != null) {
            soundManager.stop(ambience);
            ambience = null;
        }
        if (ticks > 0) {
            ambience = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    com.example.examplemod.registry.ModSounds.HACK_AMBIENCE.get(), 1.0F, 0.8F);
            soundManager.play(ambience);
        }
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
