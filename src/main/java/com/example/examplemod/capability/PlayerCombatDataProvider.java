package com.example.examplemod.capability;

import com.example.examplemod.combat.PlayerCombatData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerCombatDataProvider implements ICapabilitySerializable<CompoundTag> {

    // (A) 이 사물함을 부르는 "주소표" — 다른 파일에서 이걸로 사물함을 찾음
    public static Capability<PlayerCombatData> PLAYER_COMBAT_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    // (B) 실제 내용물 (단계 1에서 만든 그릇)
    private PlayerCombatData data = null;
    private final LazyOptional<PlayerCombatData> optional = LazyOptional.of(this::getOrCreate);

    private PlayerCombatData getOrCreate() {
        if (data == null) {
            data = new PlayerCombatData();
        }
        return data;
    }

    // (C) "사물함 주세요" 요청이 오면 내용물을 건네줌
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_COMBAT_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    // (D) 저장: 마크가 "저장해" 하면 단계1의 saveNBTData 호출
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        getOrCreate().saveNBTData(nbt);
        return nbt;
    }

    // (E) 로드: 마크가 "불러와" 하면 단계1의 loadNBTData 호출
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        getOrCreate().loadNBTData(nbt);
    }
}