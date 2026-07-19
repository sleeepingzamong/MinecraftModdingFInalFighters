package com.example.examplemod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class KnifeItem extends SwordItem {

    private static final UUID REACH_MODIFIER_UUID =
            UUID.fromString("3c1a9f2b-7d4e-4c5a-8b6f-333333333333");

    private final Multimap<Attribute, AttributeModifier> customAttributes;

    public KnifeItem(Properties properties) {
        // 공격력 4 (나무 0 + 3 + 1), 공격속도 3.6회/초 (일반 검 1.6회/초)
        super(Tiers.WOOD, 3, -0.4F, properties);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));

        // 사거리 -0.5칸 (기본 3.0 → 2.5) — 여의봉 reach 보정의 반대
        builder.put(
                ForgeMod.ENTITY_REACH.get(),
                new AttributeModifier(
                        REACH_MODIFIER_UUID,
                        "Knife reach penalty",
                        -0.5D,
                        AttributeModifier.Operation.ADDITION
                )
        );

        this.customAttributes = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.customAttributes;
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;   // 내구도 없음 (안 부서짐)
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("빠른 연속 공격").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("사거리가 짧습니다.").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}
