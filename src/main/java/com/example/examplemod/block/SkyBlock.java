package com.example.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

// 하늘 블록: 블록 자체는 아무것도 그리지 않고, 클라이언트가 하늘 렌더 직후에
// 이 블록 면의 "깊이"만 기록한다 → 뒤의 지형·엔티티가 그려지지 않아 하늘만 보임.
// (렌더링: client/SkyBlockRenderEvents, 위치 추적: SkyBlockEntity)
public class SkyBlock extends BaseEntityBlock {

    public SkyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;   // 청크 메시에는 안 그림 — 하늘 마스크가 대신 처리
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SkyBlockEntity(pos, state);
    }
}
