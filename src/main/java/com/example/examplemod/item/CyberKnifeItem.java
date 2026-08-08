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

public class CyberKnifeItem extends SwordItem {

    public CyberKnifeItem(Properties properties) {
        // 공격력 5 (1+2+2), 공격속도 3.2회/초, 사거리 기본 — "평타 중요" 컨셉
        // (도적 단검: 공4·공속3.6·사거리 2.5 — 해커 쪽이 조금 느리지만 세고 길다)
        super(Tiers.IRON, 2, -0.8F, properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;   // 내구도 없음 (안 부서짐)
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("해커의 전용 무기").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("빠른 평타로 시스템을 벤다").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}
