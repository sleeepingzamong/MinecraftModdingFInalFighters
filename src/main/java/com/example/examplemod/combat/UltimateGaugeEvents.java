package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class UltimateGaugeEvents {

    // 궁극기 자체 데미지로는 시전자 게이지가 안 차게 하는 스위치
    // (궁극기 데미지 직전에 켜고 직후에 끔 — 서버 스레드에서 순차 실행이라 안전)
    private static boolean ultimateDamage = false;

    public static void setUltimateDamage(boolean on) {
        ultimateDamage = on;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer damagedPlayer) {
            UltimateGaugeManager.addGauge(damagedPlayer, 2);   // 맞은 쪽은 궁극기에 맞아도 +2 (기존 규칙 유지)
        }

        if (ultimateDamage) {
            return;   // 궁극기 데미지 → 때린 쪽 게이지 충전 없음 (궁으로 궁 재충전 방지)
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attackerPlayer) {
            UltimateGaugeManager.addGauge(attackerPlayer, 2);
        }
    }
}