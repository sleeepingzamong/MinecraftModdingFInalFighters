package com.example.examplemod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class BattleAxeItem extends SwordItem {

    public BattleAxeItem(Properties properties) {
        // 공격력 6 (2+3+1), 공격속도 1.0회/초 — 느리지만 한 방이 무거운 양손 도끼 (분노 보너스가 본체)
        super(Tiers.IRON, 3, -3.0F, properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;   // 내구도 없음 (안 부서짐)
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("광전사의 전용 무기").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("피에 굶주린 양손 도끼").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}
