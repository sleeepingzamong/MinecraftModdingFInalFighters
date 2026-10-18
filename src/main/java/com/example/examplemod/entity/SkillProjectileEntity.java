package com.example.examplemod.entity;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.combat.CharacterType;
import com.example.examplemod.combat.CombatSync;
import com.example.examplemod.combat.DojukMarkEvents;
import com.example.examplemod.combat.GamblerEvents;
import com.example.examplemod.combat.SkillType;
import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        // 해머 투척만 포물선 낙하 (0.03 = 화살보다 완만), 나머지는 직선
        if (this.getItem().is(ModItems.HAMMER_THROWN.get())) {
            return 0.03F;
        }
        return 0.0F;
    }

    public void setMaxDistance(double distance) {
        // 0 이하 = 사거리 무제한 (그냥 제곱하면 -1이 +1이 되는 함정이 있어서 명시 처리)
        this.maxDistanceSqr = distance <= 0 ? -1.0 : distance * distance;
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

    // 지난 틱 위치~현재 위치 사이를 촘촘히 채움 → 빠르게 날아도 끊기지 않는 불꽃 선
    private void spawnFeatherTrail() {
        double dx = this.getX() - this.xo;
        double dy = this.getY() - this.yo;
        double dz = this.getZ() - this.zo;
        for (int i = 0; i < 4; i++) {
            double t = i / 4.0;
            this.level().addParticle(ParticleTypes.FLAME,
                    this.xo + dx * t, this.yo + dy * t, this.zo + dz * t, 0.0, 0.01, 0.0);
        }
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.SMALL_FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.04, 0.03, (this.random.nextDouble() - 0.5) * 0.04);
        }
    }

    // 불사조 깃털(Z): 명중 시 화상 (아이템 종류로 식별)
    private void recordPhoenixBurn(Entity victim) {
        if (this.getItem().is(ModItems.PHOENIX_FEATHER.get())
                && victim instanceof LivingEntity living
                && this.getOwner() instanceof ServerPlayer shooter) {
            com.example.examplemod.combat.PhoenixEvents.applyBurn(shooter, living);
        }
    }

    // 리볼버 총알 헤드샷: 탄이 대상 눈높이 부근(머리)에 맞았으면 true
    private boolean isHeadshot(Entity victim) {
        if (!this.getItem().is(ModItems.BULLET.get())) {
            return false;   // 헤드샷은 총알만
        }
        if (!(victim instanceof LivingEntity living)) {
            return false;
        }
        return this.getY() >= living.getEyeY() - 0.1D;
    }

    private static final float HEADSHOT_BONUS = 1.0F;   // 헤드샷 추가 데미지

    // 헤드샷 데미지 계산 + 크리티컬 연출 (별 파티클 + 타격음)
    private float applyHeadshot(Entity victim) {
        float dealt = this.damage;
        if (isHeadshot(victim)) {
            dealt += HEADSHOT_BONUS;
            if (this.level() instanceof ServerLevel serverLevel && victim instanceof LivingEntity living) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        living.getX(), living.getEyeY() + 0.2D, living.getZ(),
                        12, 0.3D, 0.2D, 0.3D, 0.25D);
                serverLevel.playSound(null, living.getX(), living.getEyeY(), living.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        }
        return dealt;
    }

    // 궤적 빛점 1개 전송 — 기본 32칸 거리 제한을 넘겨서 멀리서도 보이게 플레이어별 강제 전송
    private void sendTrailParticle(ServerLevel level, double x, double y, double z) {
        for (ServerPlayer viewer : level.players()) {
            level.sendParticles(viewer, net.minecraft.core.particles.ParticleTypes.END_ROD, true,
                    x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    // 해머 투척(Z): 명중 시 스턴 + "땅!" 소리
    private void recordHammerStun(Entity victim) {
        if (!this.getItem().is(ModItems.HAMMER_THROWN.get())) {
            return;
        }
        if (victim instanceof LivingEntity living && this.level() instanceof ServerLevel serverLevel) {
            com.example.examplemod.combat.StunManager.stun(living, com.example.examplemod.combat.HammerSkills.THROW_STUN_TICKS);
            serverLevel.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 1.6F);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    living.getX(), living.getEyeY() + 0.3D, living.getZ(),
                    10, 0.3D, 0.2D, 0.3D, 0.2D);
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
    // 불사조 깃털(Z): 불꽃 꼬리는 각 클라가 직접 그림 (패킷 없음). 처음 2틱은 쏜 사람 눈앞이라 생략
    if (this.level().isClientSide && this.isAlive() && this.tickCount > 2
            && this.getItem().is(ModItems.PHOENIX_FEATHER.get())) {
        spawnFeatherTrail();
    }
    if (this.level().isClientSide || !this.isAlive()) {
        return;
    }

    // 해머 투척: 회전하는 "해머 머리"를 따라 빛점을 남김 → 궤적이 나선(뺑글뺑글)으로 그려짐
    // 회전 각도는 렌더러(SkillProjectileRenderer)의 틱당 -40도와 같은 공식으로 계산
    if (this.getItem().is(ModItems.HAMMER_THROWN.get()) && this.level() instanceof ServerLevel serverLevel) {
        // 연출용 과장 값 (B안): 실제 모델 회전(-40)보다 빠르게 돌려서 고리가 확실히 맺히게
        final double spinDegPerTick = -120.0;  // 궤적 회전 속도 (모델과 독립)
        final double headRadius = 1.5;         // 고리 반지름 (블록)
        final double centerYOffset = 0.4;      // 나선 중심 높이 보정
        final int samplesPerTick = 4;          // 틱당 샘플 수 — 고리가 매끈하게

        Vec3 vel = this.getDeltaMovement();
        Vec3 forward = new Vec3(vel.x, 0.0, vel.z);
        forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : forward.normalize();

        for (int i = 0; i < samplesPerTick; i++) {
            double step = (double) i / samplesPerTick;
            double sampleTick = this.tickCount - step;
            double angle = Math.toRadians(sampleTick * spinDegPerTick);
            Vec3 base = this.position().subtract(vel.scale(step));
            sendTrailParticle(serverLevel,
                    base.x + forward.x * Math.sin(angle) * headRadius,
                    base.y + centerYOffset + Math.cos(angle) * headRadius,
                    base.z + forward.z * Math.sin(angle) * headRadius);
        }
    }

    // 원숭이 궁 "여래신장" 부처: 금빛 기운 — 노란 이펙트 여러 개가 주위를 감쌈
    if (this.getItem().is(ModItems.BUDDA.get()) && this.level() instanceof ServerLevel serverLevel) {
        final net.minecraft.core.particles.DustParticleOptions GOLD_DUST =
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.85F, 0.2F), 1.6F);

        // 1) 주위를 도는 금빛 점 3개 (공전 오라)
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(this.tickCount * 25.0 + i * 120.0);
            serverLevel.sendParticles(GOLD_DUST,
                    this.getX() + Math.cos(angle) * 0.9,
                    this.getY() + 0.4 + Math.sin(this.tickCount * 0.3 + i * 2.0) * 0.3,
                    this.getZ() + Math.sin(angle) * 0.9,
                    1, 0, 0, 0, 0);
        }
        // 2) 몸 주변에 퍼지는 노란 잔광
        serverLevel.sendParticles(GOLD_DUST,
                this.getX(), this.getY() + 0.4, this.getZ(), 4, 0.5, 0.5, 0.5, 0.02);
        // 3) 금빛 불꽃 반짝 (2틱마다)
        if (this.tickCount % 2 == 0) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    this.getX(), this.getY() + 0.4, this.getZ(), 2, 0.3, 0.3, 0.3, 0.01);
        }
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
            // 데미지 0짜리 연출용 투사체는 타격 처리 건너뜀
            if (this.damage > 0) {
                float dealt = this.applyHeadshot(e);
                com.example.examplemod.combat.FloatingDamageNumbers.bulletHeadshot =
                        this.getItem().is(ModItems.BULLET.get()) && this.isHeadshot(e);
                e.hurt(this.damageSources().thrown(this, this.getOwner()), dealt);
                com.example.examplemod.combat.FloatingDamageNumbers.bulletHeadshot = false;
            }
            if (ultDamage) {
                com.example.examplemod.combat.UltimateGaugeEvents.setUltimateDamage(false);
            }
            this.recordGamblerCard(e);   // 총알 데미지가 들어간 뒤 누적/폭발 판정
            this.recordHammerStun(e);
            this.recordPhoenixBurn(e);
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
    // 해머 투척만 판정 보정: 가로 +0.15, 세로 +3.0 (다른 투사체엔 영향 없음)
    if (this.getItem().is(ModItems.HAMMER_THROWN.get())) {
        mulX += 0.15D;
        mulZ += 0.15D;
        mulY += 3.0D;
    }
    // ↑↑↑

    double hx = base.getXsize() / 2.0D * mulX;
    double hy = base.getYsize() / 2.0D * mulY;
    double hz = base.getZsize() / 2.0D * mulZ;

    AABB scaled = new AABB(c.x - hx, c.y - hy, c.z - hz, c.x + hx, c.y + hy, c.z + hz);
    return scaled.move(0.0D, -0.5D, 0.3D);   // 오프셋 (지금 쓰던 값 유지)
}

    // 같은 팀(스코어보드)은 명중 대상에서 제외 — 아군은 투사체가 그냥 통과 (스턴·표식·데미지 전부 안 붙음)
    @Override
    protected boolean canHitEntity(Entity entity) {
        if (this.getOwner() != null && entity.isAlliedTo(this.getOwner())) {
            return false;
        }
        return super.canHitEntity(entity);
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
            // 데미지 0짜리 연출용 투사체(손오공 여의봉 등)는 타격 처리 자체를 건너뜀 (무적틱·경직도 없음)
            if (this.damage > 0) {
                float dealt = this.applyHeadshot(result.getEntity());
                // 팝업 색상용 헤드샷 플래그 (숫자 자체는 FloatingDamageNumbers가 전 피해 공통으로 띄움)
                com.example.examplemod.combat.FloatingDamageNumbers.bulletHeadshot =
                        this.getItem().is(ModItems.BULLET.get()) && this.isHeadshot(result.getEntity());
                result.getEntity().hurt(damageSources().thrown(this, getOwner()), dealt);
                com.example.examplemod.combat.FloatingDamageNumbers.bulletHeadshot = false;
            }
            if (ultDamage) {
                com.example.examplemod.combat.UltimateGaugeEvents.setUltimateDamage(false);
            }
            this.recordGamblerCard(result.getEntity());   // 총알 데미지가 들어간 뒤 누적/폭발 판정
            this.recordHammerStun(result.getEntity());
            this.recordPhoenixBurn(result.getEntity());
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
