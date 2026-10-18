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

    // 불사조 화상 도트 스위치 — 화상 데미지는 게이지를 1만 채움 (같은 켜고-끄기 패턴)
    private static boolean burnDamage = false;

    public static void setUltimateDamage(boolean on) {
        ultimateDamage = on;
    }

    public static void setBurnDamage(boolean on) {
        burnDamage = on;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        int gain = burnDamage ? 1 : 3;   // 화상 도트는 1, 일반 타격은 3

        if (event.getEntity() instanceof ServerPlayer damagedPlayer) {
            UltimateGaugeManager.addGauge(damagedPlayer, gain);   // 맞은 쪽 (궁극기에 맞아도 동일)
        }

        if (ultimateDamage) {
            return;   // 궁극기 데미지 → 때린 쪽 게이지 충전 없음 (궁으로 궁 재충전 방지)
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attackerPlayer) {
            UltimateGaugeManager.addGauge(attackerPlayer, gain);
        }

        // 사이보그 분신의 공격도 소환한 주인의 게이지를 +3
        if (event.getSource().getEntity() instanceof com.example.examplemod.entity.CloneEntity clone
                && clone.getOwnerUUID() != null
                && clone.level().getPlayerByUUID(clone.getOwnerUUID()) instanceof ServerPlayer owner) {
            UltimateGaugeManager.addGauge(owner, 3);
        }
    }
}