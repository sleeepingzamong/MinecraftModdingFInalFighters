package com.example.examplemod.entity;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.combat.CharacterType;
import com.example.examplemod.combat.CombatSync;
import com.example.examplemod.combat.DojukMarkEvents;
import com.example.examplemod.combat.GamblerEvents;
import com.example.examplemod.combat.SkillType;
import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;

public class SkillProjectileEntity extends ThrowableItemProjectile {

    private double maxDistanceSqr = -1.0;   // -1 = 제한 없음
    private Vec3 startPos;
    private float damage = 6.0F;            // 기본 데미지 (스킬1·2)
    // 복사 시스템: 이 투사체가 어느 캐릭터의 몇 번 스킬인지 (서버 전용)
    private CharacterType sourceCharacter = CharacterType.NONE;
    private SkillType sourceSlot = null;
    // 히트박스 배율 — 클라이언트까지 동기화해야 클라 박스도 같이 커짐
    private static final EntityDataAccessor<Float> DATA_HITBOX_SCALE =
            SynchedEntityData.defineId(SkillProjectileEntity.class, EntityDataSerializers.FLOAT);
    // 발사 시 찍은 시선 방향 (클라 렌더러가 이 방향으로 회전)
    private static final EntityDataAccessor<Float> DATA_LAUNCH_YAW =
            SynchedEntityData.defineId(SkillProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LAUNCH_PITCH =
            SynchedEntityData.defineId(SkillProjectileEntity.class, EntityDataSerializers.FLOAT);

    public SkillProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public SkillProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.SKILL_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HITBOX_SCALE, 1.0F);
        this.entityData.define(DATA_LAUNCH_YAW, 0.0F);
        this.entityData.define(DATA_LAUNCH_PITCH, 0.0F);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.RUYI_JINGU_BANG.get();   // ← 발사체가 보일 모습 (여의봉)
    }

    @Override
    protected float getGravity() {
        return 0.0F;   // 중력 0 → 직선 비행
    }

    public void setMaxDistance(double distance) {
        this.maxDistanceSqr = distance * distance;
        this.startPos = position();   // 발사 지점 기억
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setSource(CharacterType character, SkillType slot) {
        this.sourceCharacter = character;
        this.sourceSlot = slot;
    }

    // 맞은 대상이 플레이어면 "복사 보관함"에 이 스킬을 저장 (스킬1·2만)
    private void recordCopy(Entity victim) {
        if (this.sourceSlot != SkillType.SKILL_1 && this.sourceSlot != SkillType.SKILL_2) {
            return;   // 궁극기 등은 복사 대상 아님
        }
        if (victim instanceof ServerPlayer hitPlayer) {
            hitPlayer.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
                data.setCopiedCharacter(this.sourceCharacter);
                data.setCopiedSlot(this.sourceSlot);
            });
            CombatSync.syncStats(hitPlayer);   // HUD에 복사 스킬 갱신
        }
    }

    // 도박사: 카드 명중 → 부착 / 총알 명중 → 카드에 액면가 누적 (아이템 종류로 식별)
    private void recordGamblerCard(Entity victim) {
        if (!(victim instanceof LivingEntity living) || !(this.getOwner() instanceof ServerPlayer shooter)) {
            return;
        }
        if (this.getItem().is(ModItems.CARD.get())) {
            GamblerEvents.attachCard(shooter, living);
        } else if (this.getItem().is(ModItems.BULLET.get())) {
            GamblerEvents.onBulletHit(shooter, living, (int) this.damage);
        }
    }

    // 도적 표창(X)이면: 던진 사람에게 표식 기록 + 맞은 대상 머리 위에 단검 표시
    private void recordDojukMark(Entity victim) {
        if (this.sourceCharacter != CharacterType.DOJUK || this.sourceSlot != SkillType.SKILL_2) {
            return;
        }
        if (victim instanceof LivingEntity living && this.getOwner() instanceof ServerPlayer shooter) {
            DojukMarkEvents.applyMark(shooter, living);
        }
    }

    public void setLaunchDirection(float yaw, float pitch) {
        this.entityData.set(DATA_LAUNCH_YAW, yaw);
        this.entityData.set(DATA_LAUNCH_PITCH, pitch);
    }

    public float getLaunchYaw() {
        return this.entityData.get(DATA_LAUNCH_YAW);
    }

    public float getLaunchPitch() {
        return this.entityData.get(DATA_LAUNCH_PITCH);
    }

    public void setHitboxScale(float scale) {
        this.entityData.set(DATA_HITBOX_SCALE, scale);
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.entityData.get(DATA_HITBOX_SCALE));
    }

    // 클라에서 배율 동기화 값이 도착하면(스폰 포함) 박스를 다시 계산
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_HITBOX_SCALE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
public void tick() {
    super.tick();
    if (this.level().isClientSide || !this.isAlive()) {
        return;
    }

    // 히트박스에 닿는 적 직접 판정 (직선 판정만으론 큰 박스가 반영 안 됨)
    for (Entity e : this.level().getEntities(this, this.getBoundingBox(), this::canHitEntity)) {
        if (e instanceof LivingEntity) {
            this.recordCopy(e);
            this.recordDojukMark(e);
            boolean ultDamage = this.sourceSlot == SkillType.ULTIMATE;   // 궁극기 투사체는 시전자 게이지 충전 없음
            if (ultDamage) {
                com.example.examplemod.combat.UltimateGaugeEvents.setUltimateDamage(true);
            }
            e.hurt(this.damageSources().thrown(this, this.getOwner()), this.damage);
            if (ultDamage) {
                com.example.examplemod.combat.UltimateGaugeEvents.setUltimateDamage(false);
            }
            this.recordGamblerCard(e);   // 총알 데미지가 들어간 뒤 누적/폭발 판정
            this.discard();
            return;
        }
    }

    // 거리/수명 소멸 (기존)
    if (this.maxDistanceSqr >= 0 && this.startPos != null
            && this.position().distanceToSqr(this.startPos) >= this.maxDistanceSqr) {
        this.discard();
        return;
    }
    if (this.tickCount > 100) {
        this.discard();
    }
}
    @Override
protected AABB makeBoundingBox() {
    AABB base = super.makeBoundingBox();   // 균일 배율(setHitboxScale) 적용된 박스
    Vec3 c = base.getCenter();

    // ↓↓↓ 축별 배율 — 이 숫자만 조절하면 됨
    double mulX = 1.0D;
    double mulY = 2.0D;
    double mulZ = 1.8D;
    // ↑↑↑

    double hx = base.getXsize() / 2.0D * mulX;
    double hy = base.getYsize() / 2.0D * mulY;
    double hz = base.getZsize() / 2.0D * mulZ;

    AABB scaled = new AABB(c.x - hx, c.y - hy, c.z - hz, c.x + hx, c.y + hy, c.z + hz);
    return scaled.move(0.0D, -0.5D, 0.3D);   // 오프셋 (지금 쓰던 값 유지)
}

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            this.recordCopy(result.getEntity());
            this.recordDojukMark(result.getEntity());
            boolean ultDamage = this.sourceSlot == SkillType.ULTIMATE;   // 궁극기 투사체는 시전자 게이지 충전 없음
            if (ultDamage) {
                com.example.examplemod.combat.UltimateGaugeEvents.setUltimateDamage(true);
            }
            result.getEntity().hurt(damageSources().thrown(this, getOwner()), this.damage);
            if (ultDamage) {
                com.example.examplemod.combat.UltimateGaugeEvents.setUltimateDamage(false);
            }
            this.recordGamblerCard(result.getEntity());   // 총알 데미지가 들어간 뒤 누적/폭발 판정
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();   // 부딪히면 사라짐
        }
    }
}
