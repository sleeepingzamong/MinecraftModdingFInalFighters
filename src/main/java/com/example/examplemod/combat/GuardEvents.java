package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class GuardEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!GuardManager.isGuarding(player)) {
            return;
        }

        event.setAmount(event.getAmount() * 0.5F);   // 가드: 받는 피해 50% 감소

        // 막아냄 피드백: 방패 "텅" 소리(주변에도 들림) + 본인 화면 파란 플래시·아이콘 펄스
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.1F);
        }
        com.example.examplemod.network.ModMessages.sendToPlayer(
                new com.example.examplemod.network.GuardBlockS2CPacket(), player);
    }

        // 가드 중에는 일반 공격(데미지)도 안 나가게
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && GuardManager.isGuarding(player)) {
            event.setCanceled(true);
        }
    }
}