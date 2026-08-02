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

// 사이보그 무기 — 인파이터 컨셉: 사거리가 유일하게 기본보다 짧은 대신 체력 30 탱커가 붙어서 싸움
public class PowerKnuckleItem extends SwordItem {

    private static final UUID REACH_MODIFIER_UUID =
            UUID.fromString("2e8b5d1c-9f4a-4b7e-a3d6-555555555555");

    private final Multimap<Attribute, AttributeModifier> customAttributes;

    public PowerKnuckleItem(Properties properties) {
        super(Tiers.IRON, 2, -3.0F, properties);   // 공격력 6 (2+3+1), 공격속도 1.0회/초 — 느리고 묵직한 스트레이트

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));

        builder.put(
                ForgeMod.ENTITY_REACH.get(),
                new AttributeModifier(
                        REACH_MODIFIER_UUID,
                        "Power knuckle reach",
                        0.0D,   // 기본 사거리 3.0 (일본도·단검과 동일) — 여기 숫자로 가감 조절
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
        tooltip.add(Component.literal("사이보그의 전용 무기").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("좌클릭 잽 · 우클릭 스트레이트").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("묵직한 강철 주먹").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}
