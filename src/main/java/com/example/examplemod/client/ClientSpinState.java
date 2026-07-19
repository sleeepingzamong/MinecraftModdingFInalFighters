package com.example.examplemod.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

// 해머 회전 베기 클라 상태: "누가 언제부터 돌고 있나" (HammerSpinS2CPacket이 갱신)
// 렌더링 회전 각도 계산 + 본인일 때 3인칭 강제 전환/복원을 담당
public class ClientSpinState {

    public static final float DEGREES_PER_TICK = 18F;   // 초당 1바퀴 (360/20)

    private static final Map<Integer, Long> SPINNING = new HashMap<>();   // 엔티티 ID → 시작 게임시각
    private static CameraType prevCamera = null;   // 회전 전에 쓰던 시점 (복원용)

    public static void setSpinning(int entityId, boolean spinning) {
        Minecraft mc = Minecraft.getInstance();
        if (spinning) {
            long start = (mc.level != null) ? mc.level.getGameTime() : 0L;
            SPINNING.put(entityId, start);
            // 본인이면 3인칭(뒤)으로 강제 전환
            if (mc.player != null && mc.player.getId() == entityId) {
                prevCamera = mc.options.getCameraType();
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else {
            SPINNING.remove(entityId);
            // 본인이면 원래 시점으로 복원
            if (mc.player != null && mc.player.getId() == entityId && prevCamera != null) {
                mc.options.setCameraType(prevCamera);
                prevCamera = null;
            }
        }
    }

    public static boolean isSpinning(Entity entity) {
        return entity != null && entity.isAlive() && SPINNING.containsKey(entity.getId());
    }

    // 렌더링용 회전 각도 (부드럽게 — partialTick 보간)
    public static float getSpinAngle(Entity entity, float partialTick) {
        Long start = SPINNING.get(entity.getId());
        if (start == null) {
            return 0F;
        }
        float ticks = (entity.level().getGameTime() - start) + partialTick;
        return ticks * DEGREES_PER_TICK;
    }

    // 월드에서 나가면 전부 초기화 (잔존 상태 방지)
    public static void clearAll() {
        SPINNING.clear();
        prevCamera = null;
    }

    public static boolean hasAny() {
        return !SPINNING.isEmpty();
    }
}
