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

        event.setAmount(event.getAmount() * 0.25F);
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