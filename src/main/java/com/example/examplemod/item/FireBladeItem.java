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

// 불사조 전용 무기 — 깃털 부채(현재)·불칼(은퇴)이 같이 쓰는 클래스:
// 공격력 3, 공속 1.0회/초 (매우 느림), 명중 시 화상 (PhoenixEvents가 이 클래스로 판정해 적용)
public class FireBladeItem extends SwordItem {

    public FireBladeItem(Properties properties) {
        // 공격력 3 (나무 0+2+1), 공격속도 1.0회/초 — DPS 3 + 화상 2
        super(Tiers.WOOD, 2, -3.0F, properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;   // 내구도 없음 (안 부서짐)
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("불사조의 전용 무기").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("명중한 적을 불태웁니다 (화상 2)").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}
