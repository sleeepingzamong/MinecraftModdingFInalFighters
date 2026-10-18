package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.HitMarkerS2CPacket;
import com.example.examplemod.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 히트마커: 내 공격(평타·스킬·투사체)이 실제로 데미지를 넣으면 공격자 화면 조준점에 표시
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class HitMarkerEvents {

    // 실제 들어간 데미지 기준 (방어·패링으로 0이 되면 마커 없음)
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (attacker == event.getEntity() || event.getAmount() <= 0) {
            return;
        }
        if (event.getEntity().isAlliedTo(attacker)) {
            return;   // 아군 오사는 표시 안 함
        }
        ModMessages.sendToPlayer(new HitMarkerS2CPacket(false), attacker);
    }

    // 처치: 빨간 킬 마커
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && attacker != event.getEntity()) {
            ModMessages.sendToPlayer(new HitMarkerS2CPacket(true), attacker);
        }
    }
}
