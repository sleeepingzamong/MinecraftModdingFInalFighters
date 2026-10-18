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

// 해머 캐릭터 무기 — 공격력 최고 / 공격속도 최저 / 사거리는 여의봉(+1.5)과 카타나(+0)의 중간
// 회전 베기 자세는 playerAnimator 모션(hammer_spin.json)이 담당
public class HammerItem extends SwordItem {

    private static final UUID REACH_MODIFIER_UUID =
            UUID.fromString("4c9e7f2b-8d3a-4e51-b6c7-444444444444");
    private static final UUID DAMAGE_ADJUST_UUID =
            UUID.fromString("4c9e7f2b-8d3a-4e51-b6c7-444444444445");

    private final Multimap<Attribute, AttributeModifier> customAttributes;

    public HammerItem(Properties properties) {
        super(Tiers.IRON, 4, -3.2F, properties);   // 공격력 6.5 (2+4+1-0.5), 공격속도 0.8회/초 최저 — DPS 5.2, 대신 스턴 킷+탱커 체력

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND));

        // 공격력 -0.5 (정수 단위 생성자로는 못 넣는 소수 보정 → 최종 6.5)
        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        DAMAGE_ADJUST_UUID,
                        "Hammer damage adjust",
                        -0.5D,
                        AttributeModifier.Operation.ADDITION
                )
        );

        builder.put(
                ForgeMod.ENTITY_REACH.get(),
                new AttributeModifier(
                        REACH_MODIFIER_UUID,
                        "Hammer reach bonus",
                        0.75D,   // 여의봉(+1.5) > 해머(+0.75) > 카타나(+0)
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
        tooltip.add(Component.literal("해머의 전용 무기").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("공격 사거리 +0.75").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("느리지만 무겁게 내려칩니다.").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}
