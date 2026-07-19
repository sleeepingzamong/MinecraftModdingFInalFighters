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

public class KatanaItem extends SwordItem {

    public KatanaItem(Properties properties) {
        // 철 등급, 공격력 +3, 공격속도 2.4회/초 (단검 3.6과 여의봉 1.2의 중간)
        super(Tiers.IRON, 3, -1.6F, properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;   // 내구도 없음 (안 부서짐)
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("사이보그의 전용 무기").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("빠른 베기 공격").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}