package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capability.PlayerCombatDataProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 플레이어 틱 허브: 매 틱 처리를 한 곳에 모아 capability 조회를 1번만 한다.
// (예전엔 스태미나재생/레이저/원숭이궁/도적/배고픔이 각자 핸들러 + 각자 조회였음)
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class PlayerTickHub {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        NoHungerEvents.tick(player);   // capability 필요 없음

        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            StaminaRegenEvents.tick(player, data);
            LaserBeamEvents.tick(player, level, data);
            MonkeyUltimateEvents.tick(player, data);
            DojukMarkEvents.tick(player, level, data);
            GamblerEvents.tick(player, level, data);
            HammerSpinEvents.tick(player, level, data);
            com.example.examplemod.item.RevolverItem.ensureNextDamageRolled(player, data);
        });
    }
}
