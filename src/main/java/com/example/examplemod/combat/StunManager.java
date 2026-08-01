package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 범용 스턴 시스템 (해머 Z 투척·궁 등에서 사용, 사무라이 그로기와는 별개)
// - 플레이어: 이동 거의 불가 + 스킬·대쉬 봉인 (SkillManager가 isStunned 확인)
// - 몹: 강한 슬로우 효과
// - breakable 스턴은 "공격당하면 즉시 깨어남" (해머 궁 사양)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class StunManager {

    private static final Map<UUID, Long> STUNNED_UNTIL = new HashMap<>();   // 플레이어 스턴
    private static final Map<UUID, Long> MOB_STUNNED_UNTIL = new HashMap<>();   // 몹 스턴 (깨우기 판정용)
    private static final Set<UUID> BREAKABLE = new HashSet<>();             // 공격당하면 깨어나는 스턴

    public static void stun(LivingEntity target, int ticks) {
        stun(target, ticks, false);
    }

    public static void stun(LivingEntity target, int ticks, boolean breakable) {
        long until = target.level().getGameTime() + ticks;
        if (target instanceof ServerPlayer player) {
            STUNNED_UNTIL.merge(player.getUUID(), until, Math::max);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true));
            MOB_STUNNED_UNTIL.merge(target.getUUID(), until, Math::max);
        }
        if (breakable) {
            BREAKABLE.add(target.getUUID());
        } else {
            BREAKABLE.remove(target.getUUID());
        }
    }

    public static boolean isStunned(ServerPlayer player) {
        Long until = STUNNED_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    public static void clear(ServerPlayer player) {
        STUNNED_UNTIL.remove(player.getUUID());
        BREAKABLE.remove(player.getUUID());
    }

    // breakable 스턴: 공격당하면 즉시 깨어남
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        UUID id = victim.getUUID();
        if (!BREAKABLE.contains(id)) {
            return;
        }

        long now = victim.level().getGameTime();
        if (victim instanceof ServerPlayer player) {
            Long until = STUNNED_UNTIL.get(id);
            if (until != null && now < until) {
                STUNNED_UNTIL.remove(id);
                BREAKABLE.remove(id);
            }
        } else {
            Long until = MOB_STUNNED_UNTIL.get(id);
            if (until != null && now < until) {
                victim.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                MOB_STUNNED_UNTIL.remove(id);
                BREAKABLE.remove(id);
            }
        }
    }

    // PlayerTickHub가 매 틱 호출: 지속 슬로우 + 별 파티클
    public static void tick(ServerPlayer player, ServerLevel level) {
        Long until = STUNNED_UNTIL.get(player.getUUID());
        if (until == null) {
            return;
        }
        long now = level.getGameTime();
        if (now >= until) {
            STUNNED_UNTIL.remove(player.getUUID());
            BREAKABLE.remove(player.getUUID());
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 5, false, false));
        if (now % 8 == 0) {
            level.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getEyeY() + 0.5D, player.getZ(),
                    2, 0.25D, 0.1D, 0.25D, 0.02D);
        }
    }
}
