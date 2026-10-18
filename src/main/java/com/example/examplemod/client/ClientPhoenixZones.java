package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// 불사조 궁 장판 바닥 문양: 천천히 도는 바깥 고리 + 시전 방향을 바라보는 불사조.
// 지형까지 그려진 시점에 바닥 바로 위에 납작한 판으로 그림 (풀 밝기 — 밤에도 빛나는 문양).
// 펼쳐질 때 커지며 나타나고, 1초마다(회복 순간) 밝게 맥동하고, 끝날 때 흐려지며 사라짐.
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ClientPhoenixZones {

    private static final ResourceLocation RING =
            new ResourceLocation(ExampleMod.MODID, "textures/misc/phoenix_sigil_ring.png");
    private static final ResourceLocation BIRD =
            new ResourceLocation(ExampleMod.MODID, "textures/misc/phoenix_sigil_bird.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float BASE_ALPHA = 0.95F;          // 거의 불투명 (낮추면 반투명해짐)
    private static final float RING_SPIN = 0.0105F;         // 고리 회전 (라디안/틱 ≈ 초당 12도)
    private static final int APPEAR_TICKS = 8;              // 펼쳐지는 시간
    private static final int FADE_TICKS = 10;               // 사라지는 시간
    private static final int PILLAR_TICKS = 30;             // 장판이 깔린 뒤 불기둥이 계속 솟는 시간 (1.5초)
    private static final int BURST_FLAMES = 500;            // 시전 순간 하늘로 치솟는 불꽃 입자 수
    private static final RandomSource RANDOM = RandomSource.create();

    private static class Zone {
        final double x, y, z;
        final float radius, yaw;
        final int duration;
        int age = 0;

        Zone(double x, double y, double z, float radius, int duration, float yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.duration = duration;
            this.yaw = yaw;
        }
    }

    private static final Map<Integer, Zone> ZONES = new HashMap<>();

    public static void add(int id, double x, double y, double z, float radius, int duration, float yaw) {
        Zone zone = new Zone(x, y, z, radius, duration, yaw);
        ZONES.put(id, zone);
        spawnCastBurst(zone);
        Minecraft.getInstance().getSoundManager().play(new PhoenixZoneSound(id, x, y, z));
    }

    public static void remove(int id) {
        ZONES.remove(id);
    }

    // 장판 지속음 크기 비율(0~1): 처음 0.5초에 걸쳐 커지고 끝나기 전 0.75초에 걸쳐 작아짐. 장판이 사라졌으면 -1
    static float soundLevel(int id) {
        Zone zone = ZONES.get(id);
        if (zone == null) {
            return -1F;
        }
        float in = Math.min(1F, zone.age / 10F);
        float out = Math.min(1F, (zone.duration - zone.age) / 15F);
        return Math.max(0F, Math.min(in, out));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 일시정지 중엔 서버 장판도 멈춰 있으므로 같이 멈춤
        if (event.phase != TickEvent.Phase.END || ZONES.isEmpty() || Minecraft.getInstance().isPaused()) {
            return;
        }
        Iterator<Zone> it = ZONES.values().iterator();
        while (it.hasNext()) {
            Zone zone = it.next();
            if (++zone.age >= zone.duration) {
                it.remove();
                continue;
            }
            if (zone.age <= PILLAR_TICKS) {
                spawnPillar(zone);
            }
            spawnRing(zone);
        }
    }

    // ===== 장판 파티클: 서버는 장판 패킷 1개만 보내고 각 클라가 직접 뿌림 (입자마다 패킷을 보내던 렉 제거) =====
    // 바닐라 파티클 거리 제한(32칸)·비디오 설정의 입자 감소도 그대로 적용됨

    private static void spawn(ParticleOptions type, double x, double y, double z, double vx, double vy, double vz) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            level.addParticle(type, x, y, z, vx, vy, vz);
        }
    }

    // 시전 순간: 섬광 + 하늘로 치솟는 굵은 불기둥 + 용암 불똥 + 연기 + 반경 끝까지 바닥을 훑는 불꽃 충격파
    // (불꽃 입자는 틱마다 4%씩 감속 → 위로 속도 0.8이면 약 10칸 상승)
    private static void spawnCastBurst(Zone zone) {
        double x = zone.x, y = zone.y, z = zone.z;
        spawn(ParticleTypes.FLASH, x, y + 1.5, z, 0, 0, 0);
        for (int i = 0; i < BURST_FLAMES; i++) {
            double jx = (RANDOM.nextDouble() - 0.5) * 1.4;
            double jz = (RANDOM.nextDouble() - 0.5) * 1.4;
            double speed = 0.5 + RANDOM.nextDouble() * 0.7;
            spawn(ParticleTypes.FLAME, x + jx, y + RANDOM.nextDouble() * 1.5, z + jz,
                    jx * 0.05 * speed, speed, jz * 0.05 * speed);
        }
        for (int i = 0; i < 25; i++) {
            spawn(ParticleTypes.LAVA, x + RANDOM.nextGaussian() * 0.6, y + 1.0 + RANDOM.nextGaussian() * 1.2,
                    z + RANDOM.nextGaussian() * 0.6, 0, 0, 0);
        }
        for (int i = 0; i < 20; i++) {
            spawn(ParticleTypes.LARGE_SMOKE, x + RANDOM.nextGaussian() * 0.6, y + 2.0 + RANDOM.nextGaussian() * 1.5,
                    z + RANDOM.nextGaussian() * 0.6,
                    RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02);
        }
        for (int i = 0; i < 64; i++) {
            double a = Math.PI * 2 * i / 64.0;
            spawn(ParticleTypes.FLAME, x, y + 0.2, z, Math.cos(a) * 0.45, 0.009, Math.sin(a) * 0.45);
        }
    }

    // 장판이 깔린 뒤 1.5초 동안 중심에서 불기둥이 계속 솟음 (처음엔 굵고 점점 잦아듦)
    private static void spawnPillar(Zone zone) {
        int count = Math.max(2, 10 - zone.age / 3);
        for (int i = 0; i < count; i++) {
            double jx = RANDOM.nextDouble() - 0.5;
            double jz = RANDOM.nextDouble() - 0.5;
            double speed = 0.4 + RANDOM.nextDouble() * 0.5;
            spawn(ParticleTypes.FLAME, zone.x + jx, zone.y + 0.2, zone.z + jz,
                    jx * 0.04 * speed, speed, jz * 0.04 * speed);
        }
    }

    // 바깥 불꽃 원 = 효과 범위 그대로 (매 틱 14점씩 돌아가며 찍어 4틱에 한 바퀴) + 바닥에서 피어오르는 불씨
    private static void spawnRing(Zone zone) {
        int phase = zone.age % 4;
        for (int i = 0; i < 14; i++) {
            double a = Math.PI * 2 * (i * 4 + phase) / 56.0;
            spawn(ParticleTypes.FLAME,
                    zone.x + Math.cos(a) * zone.radius + RANDOM.nextGaussian() * 0.05,
                    zone.y + 0.1 + RANDOM.nextGaussian() * 0.05,
                    zone.z + Math.sin(a) * zone.radius + RANDOM.nextGaussian() * 0.05,
                    RANDOM.nextGaussian() * 0.005, RANDOM.nextGaussian() * 0.005, RANDOM.nextGaussian() * 0.005);
        }
        for (int i = 0; i < 3; i++) {
            double r = zone.radius * Math.sqrt(RANDOM.nextDouble());
            double a = RANDOM.nextDouble() * Math.PI * 2;
            spawn(ParticleTypes.SMALL_FLAME, zone.x + Math.cos(a) * r, zone.y + 0.1, zone.z + Math.sin(a) * r,
                    0, 0.04, 0);
        }
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS || ZONES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();

        for (Zone zone : ZONES.values()) {
            float t = zone.age + partial;
            float appear = Math.min(1.0F, t / APPEAR_TICKS);
            float scale = 0.4F + 0.6F * (1.0F - (1.0F - appear) * (1.0F - appear));   // ease-out으로 펼쳐짐
            float fade = Math.min(1.0F, Math.max(0.0F, (zone.duration - t) / FADE_TICKS));
            float pulse = (float) Math.exp(-Math.pow((t % 20.0F) / 5.0F, 2));         // 1초마다 회복 순간 반짝
            float alpha = BASE_ALPHA * Math.min(1.0F, t / 6.0F) * fade * (0.9F + 0.1F * pulse);
            float bright = 0.85F + 0.15F * pulse;
            if (alpha <= 0.01F) {
                continue;
            }
            float half = zone.radius * scale;
            float cx = (float) (zone.x - cam.x);
            float cz = (float) (zone.z - cam.z);

            pose.pushPose();
            PoseStack.Pose last = pose.last();
            // 고리는 천천히 회전, 불사조는 시전자가 바라보던 방향으로 머리를 둠 (바닥과 z파이팅 없게 살짝 띄움)
            quad(buffer.getBuffer(RenderType.entityTranslucent(RING)), last,
                    cx, (float) (zone.y + 0.03 - cam.y), cz, half, t * RING_SPIN, bright, alpha);
            quad(buffer.getBuffer(RenderType.entityTranslucent(BIRD)), last,
                    cx, (float) (zone.y + 0.04 - cam.y), cz, half, (float) Math.toRadians(zone.yaw), bright, alpha);
            pose.popPose();
        }
        buffer.endBatch();
    }

    // 바닥에 눕힌 정사각형: angle 방향(마크 yaw 규칙)이 텍스처 위쪽(v=0)
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float cx, float y, float cz,
                             float half, float angle, float bright, float alpha) {
        float fx = (float) -Math.sin(angle), fz = (float) Math.cos(angle);    // 앞
        float rx = (float) -Math.cos(angle), rz = (float) -Math.sin(angle);   // 오른쪽
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vertex(vc, m, n, cx + (-rx + fx) * half, y, cz + (-rz + fz) * half, 0, 0, bright, alpha);
        vertex(vc, m, n, cx + (rx + fx) * half, y, cz + (rz + fz) * half, 1, 0, bright, alpha);
        vertex(vc, m, n, cx + (rx - fx) * half, y, cz + (rz - fz) * half, 1, 1, bright, alpha);
        vertex(vc, m, n, cx + (-rx - fx) * half, y, cz + (-rz - fz) * half, 0, 1, bright, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z,
                               float u, float v, float bright, float alpha) {
        vc.vertex(m, x, y, z)
                .color(bright, bright, bright, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ZONES.clear();
    }
}
