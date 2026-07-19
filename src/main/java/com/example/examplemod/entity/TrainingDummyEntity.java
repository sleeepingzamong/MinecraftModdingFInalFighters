package com.example.examplemod.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

// 훈련용 좀비(더미): AI 없음, 체력 1000, 머리 위에 "총 | 직전" 데미지 표시
public class TrainingDummyEntity extends Zombie {

    private float totalDamage = 0.0F;
    private float lastDamage = 0.0F;

    public TrainingDummyEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();     // 자연 디스폰 방지
        this.updateNameTag();
        this.setCustomNameVisible(true);   // 이름표 항상 보이게
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)   // 때려도 안 밀림
                .add(Attributes.ARMOR, 0.0D);                 // 갑옷 0 → 표시 데미지 = 실제 깎인 체력
    }

    @Override
    protected void registerGoals() {
        // AI 없음: 아무 목표도 등록하지 않음 (이동·공격 안 함)
    }

    @Override
    protected boolean isSunSensitive() {
        return false;   // 햇빛에 안 탐 (데미지 수치 오염 방지)
    }

    // hurt()의 인자는 보너스(투명 +2 등)가 붙기 "전" 값이라, 실제 깎인 체력으로 측정한다
    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        float before = this.getHealth();
        super.actuallyHurt(source, amount);       // LivingHurtEvent 보너스까지 다 적용됨
        float dealt = before - this.getHealth();  // 실제 깎인 체력
        if (dealt > 0 && !this.level().isClientSide) {
            this.lastDamage = dealt;
            this.totalDamage += dealt;
            this.updateNameTag();
        }
    }

    private void updateNameTag() {
        this.setCustomName(Component.literal(
                String.format("총 %.1f | 직전 %.1f", this.totalDamage, this.lastDamage)));
    }
}
