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

public class CyberScytheItem extends SwordItem {

    public CyberScytheItem(Properties properties) {
        // 공격력 6 (2+3+1), 공격속도 1.2회/초 — DPS 7.2
        super(Tiers.IRON, 3, -2.8F, properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;   // 내구도 없음 (안 부서짐)
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("해커의 전용 무기").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("데이터를 수확하는 사이버 낫").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}
