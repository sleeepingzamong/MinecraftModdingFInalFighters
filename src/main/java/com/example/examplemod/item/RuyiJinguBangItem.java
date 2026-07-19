package com.example.examplemod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraft.world.item.UseAnim;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class RuyiJinguBangItem extends SwordItem {

    private static final UUID REACH_MODIFIER_UUID =
            UUID.fromString("8b6f2d1a-6b0a-4d8f-9e2a-111111111111");

    private final Multimap<Attribute, AttributeModifier> customAttributes;

    public RuyiJinguBangItem(Properties properties) {
        super(Tiers.WOOD, 4, -2.8F, properties);   // 공격력 5 (0+4+1), 공격속도 1.2회/초 (제일 느림, 대신 사거리 김)

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));

        builder.put(
                ForgeMod.ENTITY_REACH.get(),
                new AttributeModifier(
                        REACH_MODIFIER_UUID,
                        "Ruyi Jingu Bang reach bonus",
                        1.5D,
                        AttributeModifier.Operation.ADDITION
                )
        );

        this.customAttributes = builder.build();
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;   // 내구도 없음 (안 부서짐)
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.customAttributes;
        }

        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("원숭이의 전용 무기").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("공격 사거리 +1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("긴 봉으로 멀리 있는 적을 공격합니다.").withStyle(ChatFormatting.DARK_GRAY));

        super.appendHoverText(itemStack, level, tooltip, flag);
    }
    @Override
public boolean hurtEnemy(ItemStack itemStack, LivingEntity target, LivingEntity attacker) {
    Vec3 knockbackDirection = target.position().subtract(attacker.position()).normalize();

    target.push(
            knockbackDirection.x * 0.1,
            0.05,
            knockbackDirection.z * 0.1
    );

    return super.hurtEnemy(itemStack, target, attacker);
    }
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }
}