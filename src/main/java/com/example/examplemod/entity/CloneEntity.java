package com.example.examplemod.entity;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 플레이어의 사이보그 스킬로 소환되는 임시 "분신" 몹.
 *  - 소환한 플레이어의 스킨으로 렌더링된다(클라이언트에서 ownerUUID로 스킨을 찾음).
 *  - 주변 적대 몹(Monster)을 근접 공격한다(바닐라 AI).
 *  - 200틱(10초) 뒤 자동으로 사라진다.
 *
 * PathfinderMob을 상속한다(Monster가 아님): Monster를 상속하면 Enemy 인터페이스가 붙어
 * 다른 적대 몹이 이 분신을 아군으로 취급해 공격하지 않을 수 있다. 중립인 PathfinderMob이 맞다.
 */
public class CloneEntity extends PathfinderMob {

    // 소환한 플레이어의 UUID. 클라이언트 렌더러가 이 값으로 스킨을 조회한다.
    // 바닐라 TamableAnimal.DATA_OWNERUUID_ID와 동일한 패턴.
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final int LIFETIME_TICKS = 200; // 10초 * 20tps

    public CloneEntity(EntityType<? extends CloneEntity> type, Level level) {
        super(type, level);
        // 타이머가 지우기 전에 바닐라 자연 디스폰이 먼저 지우지 못하게 한다.
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    /**
     * 능력치 빌더. ModEntities.onAttributeCreation 에서 EntityAttributeCreationEvent로 등록한다.
     * createMobAttributes()가 MAX_HEALTH/MOVEMENT_SPEED/FOLLOW_RANGE 기본값을 이미 넣으므로,
     * .add(attr, value)는 그 기본값을 덮어쓴다(안전한 표준 방식).
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)   // 기본 3 + 너클 보정(+2) = 타격당 5
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        // 행동 목표
        this.goalSelector.addGoal(0, new FloatGoal(this));                              // 물에서 뜨기
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));            // 근접 추격/공격
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));    // 평소 배회
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));   // 플레이어 쳐다보기
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));                   // 주위 둘러보기

        // 대상 목표
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));                     // 맞으면 반격
        // 근처의 적 플레이어를 능동적으로 추격·공격 (소환자·같은 팀은 제외)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> !target.getUUID().equals(this.getOwnerUUID()) && !isOwnerAlly(target)));
        // 주변 적대 몹(Monster)도 자동으로 노린다. mustSee = true.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    // 소환자(그리고 소환자와 같은 팀)는 절대 공격 대상이 되지 않음
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && (target.getUUID().equals(this.getOwnerUUID()) || isOwnerAlly(target))) {
            return;
        }
        super.setTarget(target);
    }

    // 플레이어처럼 fallDamage 게임룰을 따름 — /gamerule fallDamage false면 분신도 낙하 데미지 없음
    // (바닐라는 이 게임룰을 플레이어에게만 적용하고 몹은 무시한다)
    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        if (!this.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_FALL_DAMAGE)) {
            return false;
        }
        return super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target.getUUID().equals(this.getOwnerUUID()) || isOwnerAlly(target)) {
            return false;
        }
        return super.canAttack(target);
    }

    // 소환자와 같은 팀(스코어보드)인지
    private boolean isOwnerAlly(net.minecraft.world.entity.Entity target) {
        if (this.getOwnerUUID() == null) {
            return false;
        }
        Player owner = this.level().getPlayerByUUID(this.getOwnerUUID());
        return owner != null && target.isAlliedTo(owner);
    }

    // 소환자 본인·아군의 공격은 분신에게 안 통함 (오폭으로 분신이 죽지 않게)
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() != null) {
            if (source.getEntity().getUUID().equals(this.getOwnerUUID())
                    || isOwnerAlly(source.getEntity())) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        // tickCount는 Entity의 public int. 서버에서만 타이머를 돌린다.
        if (!this.level().isClientSide && this.tickCount >= LIFETIME_TICKS) {
            this.discard();
        }
    }

    // 타이머가 제거를 담당하므로 바닐라 거리-기반 디스폰은 끈다.
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // 소환자 UUID를 저장/로드 (Mob에서 public 오버라이드). TamableAnimal의 "Owner" 태그 패턴.
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwnerUUID() != null) {
            tag.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.setOwnerUUID(tag.getUUID("Owner"));
        }
    }
}
