package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Set;

// 하늘 블록 렌더링: 하늘을 그린 직후(지형을 그리기 전)에 블록 면의 깊이만 기록한다.
// → 그 픽셀에는 이후 지형·엔티티·구름이 그려지지 않아 하늘만 보인다.
//
// "창문 모드": 하늘 블록이 화면에서 차지하는 실루엣만 정확히 하늘로 뚫린다.
// 실루엣은 원근 투영이 시점에 맞춰 알아서 계산하므로 시선 추적이 필요 없고,
// 예전처럼 시선 광선을 따라 뒤쪽 칸을 골라 지우던 방식의 팝핑·틈·거리 제한이 없다.
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class SkyBlockRenderEvents {

    /** 마스크 큐브를 살짝 부풀려 가려질 블록 면과 깊이가 같아지는(z-파이팅) 걸 막는다 */
    private static final float INFLATE = 0.01F;

    // 마스킹할 칸들 (BlockPos.asLong) — 붙어 있는 하늘 블록끼리 맞닿은 면 생략용. 렌더 스레드 전용.
    private static final LongOpenHashSet MASK = new LongOpenHashSet();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Set<BlockPos> blocks = ClientSkyBlocks.all();
        if (blocks.isEmpty()) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();

        MASK.clear();
        for (BlockPos pos : blocks) {
            MASK.add(pos.asLong());
        }

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(ModRenderTypes.SKY_MASK);

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        LongIterator it = MASK.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            addCube(vc, m, BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
        }
        pose.popPose();
        buffers.endBatch(ModRenderTypes.SKY_MASK);
    }

    // 큐브 6면. 이웃 칸도 마스킹 대상이면 그 사이 면은 생략 (붙어 있을 때 낭비 제거)
    private static void addCube(VertexConsumer vc, Matrix4f m, int px, int py, int pz) {
        float x0 = px - INFLATE, y0 = py - INFLATE, z0 = pz - INFLATE;
        float x1 = px + 1 + INFLATE, y1 = py + 1 + INFLATE, z1 = pz + 1 + INFLATE;
        if (!MASK.contains(BlockPos.asLong(px, py - 1, pz))) {
            quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        }
        if (!MASK.contains(BlockPos.asLong(px, py + 1, pz))) {
            quad(vc, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        }
        if (!MASK.contains(BlockPos.asLong(px, py, pz - 1))) {
            quad(vc, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        }
        if (!MASK.contains(BlockPos.asLong(px, py, pz + 1))) {
            quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        }
        if (!MASK.contains(BlockPos.asLong(px - 1, py, pz))) {
            quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        }
        if (!MASK.contains(BlockPos.asLong(px + 1, py, pz))) {
            quad(vc, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vc.vertex(m, ax, ay, az).endVertex();
        vc.vertex(m, bx, by, bz).endVertex();
        vc.vertex(m, cx, cy, cz).endVertex();
        vc.vertex(m, dx, dy, dz).endVertex();
    }

    // 월드에서 나가면 위치 기억을 비움
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSkyBlocks.clearAll();
        MASK.clear();
    }
}
