package com.example.examplemod.block;

import com.example.examplemod.client.ClientSkyBlocks;
import com.example.examplemod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// 하늘 블록 위치 추적용 — 클라이언트에서 로드/제거될 때 렌더 목록에 등록/해제만 한다.
// (ClientSkyBlocks는 순수 자료구조라 서버에서 클래스가 로드돼도 안전)
public class SkyBlockEntity extends BlockEntity {

    public SkyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SKY_BLOCK.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide) {
            ClientSkyBlocks.add(this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && this.level.isClientSide) {
            ClientSkyBlocks.remove(this.worldPosition);
        }
    }
}
