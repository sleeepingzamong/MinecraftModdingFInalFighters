package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 커스텀 체력 재생: 초당 1 (0.5초마다 반 칸), 피격 후 3.5초 동안은 회복 정지
// 바닐라 자연 재생(배고픔 기반, 초당 ~1.7)은 월드 시작 시 게임룰로 꺼서 이걸로 대체한다.
// 검사 흡혈 등 스킬에 의한 회복은 이 지연의 영향을 받지 않는다 (직접 heal 호출).
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class HealthRegenEvents {

    private static final int COMBAT_DELAY_TICKS = 70;   // 피격 후 3.5초간 재생 정지
    private static final int REGEN_INTERVAL = 10;       // 0.5초마다
    private static final float REGEN_AMOUNT = 0.5F;     // 반 칸씩 → 초당 1

    private static final Map<UUID, Long> LAST_HURT = new HashMap<>();

    // 피해를 입으면 재생 타이머 리셋
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0) {
            LAST_HURT.put(player.getUUID(), player.level().getGameTime());
        }
    }

    // 월드 시작 시 바닐라 자연 재생 게임룰 끄기 (커스텀 재생으로 완전 대체)
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        event.getServer().getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION)
                .set(false, event.getServer());
    }

    // PlayerTickHub가 매 틱 호출
    public static void tick(ServerPlayer player, ServerLevel level) {
        if (level.getGameTime() % REGEN_INTERVAL != 0) {
            return;
        }
        if (!player.isAlive() || player.isSpectator()
                || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        long lastHurt = LAST_HURT.getOrDefault(player.getUUID(), -1_000_000L);
        if (level.getGameTime() - lastHurt < COMBAT_DELAY_TICKS) {
            return;   // 전투 중 (맞은 지 3.5초 안 지남)
        }
        FloatingDamageNumbers.passiveRegen = true;   // 자연 회복은 숫자 팝업 생략
        player.heal(REGEN_AMOUNT);
        FloatingDamageNumbers.passiveRegen = false;
    }
}
