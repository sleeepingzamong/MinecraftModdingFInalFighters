package com.example.examplemod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

// 커스텀 렌더 타입 모음 (RenderType을 상속해야 protected 상태 조각에 접근 가능)
public class ModRenderTypes extends RenderType {

    private ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                           boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    // 하늘 마스크: 색은 안 쓰고 깊이만 기록 → 이 픽셀 뒤의 지형/엔티티가 그려지지 않아
    // 하늘(먼저 그려진 것)만 남는다. 하늘 렌더 직후(AFTER_SKY)에 그려야 효과가 있음.
    public static final RenderType SKY_MASK = create("examplemod_sky_mask",
            DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, false, false,
            CompositeState.builder()
                    .setShaderState(POSITION_SHADER)
                    .setWriteMaskState(new WriteMaskStateShard(false, true))
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
}
