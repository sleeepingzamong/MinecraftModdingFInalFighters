package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 생명력 강화(바닐라 효과)가 새로 걸리거나 레벨이 오르면, 늘어난 최대 체력만큼 바로 채움.
// 바닐라는 늘어난 칸을 빈 칸으로 줘서, 부활 직후 커맨드로 다시 걸면 그 칸이 빈 채로 시작했음.
// 같은 레벨로 다시 거는 갱신(매 틱 반복 커맨드 블록)은 무시 → 계속 회복되는 일 없음.
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class HealthBoostFillEvents {

    private static final Map<UUID, Float> PENDING = new HashMap<>();   // 이번 틱 끝에 채울 양

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance added = event.getEffectInstance();
        if (added.getEffect() != MobEffects.HEALTH_BOOST
                || !(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        MobEffectInstance old = event.getOldEffectInstance();
        int newLevels = added.getAmplifier() + 1;
        int oldLevels = old == null ? 0 : old.getAmplifier() + 1;
        if (newLevels > oldLevels) {
            PENDING.merge(player.getUUID(), 4.0F * (newLevels - oldLevels), Float::sum);   // 레벨당 체력 4
        }
    }

    // 최대 체력 증가는 이 이벤트 직후에 적용되므로, 플레이어 틱 끝에서 채움.
    // heal() 대신 체력을 직접 올려서 회복 숫자 팝업(+초록 숫자)은 띄우지 않음
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || PENDING.isEmpty()) {
            return;
        }
        Float add = PENDING.remove(event.player.getUUID());
        if (add != null && event.player.isAlive()) {
            event.player.setHealth(Math.min(event.player.getMaxHealth(), event.player.getHealth() + add));
        }
    }
}
