package com.example.examplemod.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

// 해머 캐릭터 무기 (아직 공격 스탯 없음)
// 회전 베기 중 양팔을 앞으로 뻗는 자세를 이 아이템에 연결한다 (클라 전용 훅)
public class HammerItem extends Item {

    public HammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                if (com.example.examplemod.client.ClientSpinState.isSpinning(entityLiving)) {
                    return com.example.examplemod.client.HammerPoses.SPIN;
                }
                return null;   // null = 기본 자세
            }
        });
    }
}
