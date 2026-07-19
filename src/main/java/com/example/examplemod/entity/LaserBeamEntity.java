package com.example.examplemod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

// 사이보그 레이저 빔의 "닻" 엔티티. 보이는 빔은 LaserBeamRenderer가 발사자 시선 기준으로 그린다.
public class LaserBeamEntity extends Entity {

    // 발사자 UUID — 클라 렌더러가 이걸로 발사자를 찾아 시선 방향으로 빔을 그림
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public LaserBeamEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;   // 충돌 없음
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        // 발사자를 따라다님. 발사자가 없어지거나 너무 오래되면 자기 정리 (안전장치)
        Player owner = this.getOwnerUUID().map(this.level()::getPlayerByUUID).orElse(null);
        if (owner == null || !owner.isAlive() || this.tickCount > 300) {
            this.discard();
            return;
        }
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 임시 엔티티 — 저장 안 함
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 임시 엔티티 — 저장 안 함
    }
}
