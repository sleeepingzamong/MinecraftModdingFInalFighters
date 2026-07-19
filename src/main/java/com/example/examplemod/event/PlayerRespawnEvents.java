package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.combat.CombatSync;
import com.example.examplemod.combat.GuardManager;
import com.example.examplemod.combat.PlayerCharacterManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class PlayerRespawnEvents {

    // 1) 죽어서 새 몸이 생길 때: 헌 사물함 → 새 사물함으로 내용 복사
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 죽어서 생긴 clone만 처리 (차원 이동 등은 무시)
        if (!event.isWasDeath()) {
            return;
        }

        // 죽은 몸(원본)의 사물함 잠금을 잠깐 풀어줌 
        event.getOriginal().reviveCaps();

        // 헌 사물함(oldData) → 새 사물함(newData)으로 복사
        event.getOriginal().getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(oldData ->
            event.getEntity().getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(newData ->
                newData.copyFrom(oldData)
            )
        );

        // 다시 잠가서 정리
        event.getOriginal().invalidateCaps();
    }

    // 2) 부활이 끝난 직후: 캐릭터 체력 보너스 다시 적용 + HUD 갱신
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        PlayerCharacterManager.reapplyCharacterHealth(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CombatSync.syncStats(serverPlayer);
        }
    }
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GuardManager.clear(serverPlayer);
            // 룰렛 연출 중 나가면 돔/무적이 남지 않게 정리
            com.example.examplemod.combat.GamblerEvents.onCasterGone(serverPlayer);
        }
    }

    // 3) 접속하자마자 HUD 초기값 보내기
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CombatSync.syncStats(serverPlayer);
        }
    }
}