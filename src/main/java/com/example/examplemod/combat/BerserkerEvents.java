package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.FearS2CPacket;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.registry.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 광전사 스킬 처리
// - 패시브 분노: 때리거나 맞을 때마다 분노 +1 (최대 6). 5초간 전투가 없으면 서서히 감소
// - Z 돌진 찍기: 전방 돌진, 부딪힌 적 데미지 + 넉백
// - X 피의 격노: 분노 4를 소모해 5초간 공속·이속 대폭 증가
// - 궁 광폭화: 시전 시 분노를 전부 소모 — 15초간 공격력 증가 (기본 +10% + 소모한 스택당 +10%)
//              + 받는 피해 감소, 시전 순간 주변 적 공포 (느려짐 + 머리 위 귀신 표식)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class BerserkerEvents {

    // ==================== 튜닝 상수 ====================
    private static final int DASH_TICKS = 6;                  // Z 돌진 지속 (0.3초)
    private static final double DASH_SPEED = 1.2D;            // Z 돌진 속도
    private static final float DASH_DAMAGE = 5.0F;            // Z 명중 데미지
    private static final float DASH_KNOCKBACK = 0.9F;         // Z 넉백 세기

    public static final int MAX_RAGE = 6;                     // 분노 최대 스택
    public static final int FRENZY_RAGE_COST = 4;             // X 소모 분노
    private static final int RAGE_DECAY_DELAY = 100;          // 전투 없이 5초 지나면
    private static final int RAGE_DECAY_INTERVAL = 40;        // 2초마다 분노 1 감소

    private static final int FRENZY_DURATION_TICKS = 100;     // X 지속 (5초)
    private static final double FRENZY_ATTACK_SPEED = 0.4D;   // X 공격속도 +40%
    private static final double FRENZY_MOVE_SPEED = 0.25D;    // X 이동속도 +25%

    private static final int RAGE_DURATION_TICKS = 300;       // 궁 지속 (15초)
    private static final float RAGE_BASE_BONUS = 0.10F;       // 궁 기본 공격력 +10%
    private static final float RAGE_PER_STACK = 0.10F;        // 궁 시전 시 소모한 분노 스택당 +10%
    private static final float RAGE_DAMAGE_REDUCTION = 0.2F;  // 궁 중 받는 피해 20% 감소
    private static final double FEAR_RADIUS = 8.0D;           // 공포 범위 (블록)
    private static final int FEAR_TICKS = 60;                 // 공포 지속 (3초)
    private static final int FEAR_SLOW_AMPLIFIER = 1;         // 공포 느려짐 단계 (II)
    // =================================================

    private static final UUID FRENZY_ATTACK_SPEED_UUID =
            UUID.fromString("b5e11a3c-7d2f-4c3e-9a1d-000000000551");
    private static final UUID FRENZY_MOVE_SPEED_UUID =
            UUID.fromString("b5e11a3c-7d2f-4c3e-9a1d-000000000552");

    private static final DustParticleOptions BLOOD_DUST =
            new DustParticleOptions(new Vector3f(0.75F, 0.05F, 0.05F), 1.3F);

    private static final Map<UUID, Integer> DASHING = new HashMap<>();      // Z 돌진 남은 틱
    private static final Map<UUID, Set<UUID>> DASH_HIT = new HashMap<>();   // 이번 돌진에 이미 맞은 적
    private static final Map<UUID, Long> FRENZY_UNTIL = new HashMap<>();    // X 격노 종료 시각
    private static final Map<UUID, Long> RAGE_UNTIL = new HashMap<>();      // 궁 광폭화 종료 시각
    private static final Map<UUID, Integer> RAGE_STACKS = new HashMap<>();  // 분노 스택 (0~6)
    private static final Map<UUID, Long> LAST_COMBAT = new HashMap<>();     // 마지막 전투 시각 (감소 판정)
    private static final Map<UUID, Float> RAGE_BONUS = new HashMap<>();     // 이번 광폭화의 공격력 보너스 (시전 시 고정)

    // ===== Z 돌진 찍기 =====
    public static void chargeStrike(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
        player.setDeltaMovement(dir.scale(DASH_SPEED).add(0, 0.1, 0));
        player.hurtMarked = true;
        DASHING.put(player.getUUID(), DASH_TICKS);
        DASH_HIT.put(player.getUUID(), new HashSet<>());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    // ===== X 피의 격노 (분노 4 소모) =====
    public static void bloodFrenzy(ServerPlayer player, ServerLevel level) {
        int stacks = RAGE_STACKS.getOrDefault(player.getUUID(), 0);
        if (stacks < FRENZY_RAGE_COST) {
            return;   // SkillManager가 미리 막지만, 만약을 위해 조용히 무시
        }
        setRage(player, stacks - FRENZY_RAGE_COST);

        long now = level.getGameTime();
        FRENZY_UNTIL.put(player.getUUID(), now + FRENZY_DURATION_TICKS);
        player.addEffect(new MobEffectInstance(
                com.example.examplemod.registry.ModEffects.FRENZY.get(), FRENZY_DURATION_TICKS, 0, false, false, true));
        addModifier(player, Attributes.ATTACK_SPEED, FRENZY_ATTACK_SPEED_UUID,
                "Berserker frenzy attack speed", FRENZY_ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addModifier(player, Attributes.MOVEMENT_SPEED, FRENZY_MOVE_SPEED_UUID,
                "Berserker frenzy move speed", FRENZY_MOVE_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);

        level.sendParticles(BLOOD_DUST,
                player.getX(), player.getY() + 1.0D, player.getZ(), 25, 0.5D, 0.7D, 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                player.getX(), player.getY() + 1.2D, player.getZ(), 6, 0.3D, 0.3D, 0.3D, 0.2D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6F, 1.6F);
        player.displayClientMessage(Component.literal(
                "피의 격노! (" + (FRENZY_DURATION_TICKS / 20) + "초)"), true);
    }

    // ===== 궁 광폭화 (분노 전부 소모 — 소모량 비례 보너스 고정) =====
    public static void rage(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        int stacks = RAGE_STACKS.getOrDefault(player.getUUID(), 0);
        float bonus = RAGE_BASE_BONUS + RAGE_PER_STACK * stacks;
        setRage(player, 0);
        RAGE_BONUS.put(player.getUUID(), bonus);
        RAGE_UNTIL.put(player.getUUID(), now + RAGE_DURATION_TICKS);
        player.addEffect(new MobEffectInstance(
                com.example.examplemod.registry.ModEffects.RAMPAGE.get(), RAGE_DURATION_TICKS, 0, false, false, true));

        // 공포 펄스: 주변 적(몹 포함) 느려짐 + 머리 위 귀신 표식
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(FEAR_RADIUS),
                e -> e != player && e.isAlive() && !e.isAlliedTo(player))) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, FEAR_TICKS, FEAR_SLOW_AMPLIFIER));
            level.sendParticles(ParticleTypes.SMOKE,
                    target.getX(), target.getY() + 2.0D, target.getZ(), 8, 0.25D, 0.3D, 0.25D, 0.02D);
            ModMessages.sendToTracking(new FearS2CPacket(target.getId(), FEAR_TICKS), target);
        }

        level.sendParticles(BLOOD_DUST,
                player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.8D, 1.0D, 0.8D, 0.0D);
        // 분노 불꽃 폭발 — 몸 중심에서 사방으로 뿜어져 나감
        burstRageFlames(level, player, 28, 0.45D);
        // 포효는 커스텀 사운드(berserker_ult 슬롯)가 담당 — 바닐라 라바저/엘더가디언 소리 제거됨
        int bonusPercent = (int) (bonus * 100);
        player.displayClientMessage(Component.literal(
                "광폭화! (" + (RAGE_DURATION_TICKS / 20) + "초) — 공격력 +" + bonusPercent + "%, 주변 적 공포"), true);
    }

    // 분노 축적 + 광폭화 데미지 보정 (시전 시 고정된 보너스, 받는 피해는 감소)
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && attacker != event.getEntity() && isBerserker(attacker)) {
            addRage(attacker, 1);   // 때리면 분노 +1
            if (isRaging(attacker)) {
                float bonus = RAGE_BONUS.getOrDefault(attacker.getUUID(), RAGE_BASE_BONUS);
                event.setAmount(event.getAmount() * (1.0F + bonus));
            }
        }
        if (event.getEntity() instanceof ServerPlayer victim && isBerserker(victim)) {
            if (event.getSource().getEntity() != victim) {
                addRage(victim, 1);   // 맞아도 분노 +1
            }
            if (isRaging(victim)) {
                event.setAmount(event.getAmount() * (1.0F - RAGE_DAMAGE_REDUCTION));
            }
        }
    }

    // PlayerTickHub가 매 틱 호출
    public static void tick(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        UUID id = player.getUUID();

        // Z 돌진: 궤적 파티클 + 부딪힌 적 타격 (돌진당 1회)
        Integer dashLeft = DASHING.get(id);
        if (dashLeft != null) {
            if (dashLeft <= 0 || !player.isAlive()) {
                DASHING.remove(id);
                DASH_HIT.remove(id);
            } else {
                level.sendParticles(ParticleTypes.CRIT,
                        player.getX(), player.getY() + 1.0D, player.getZ(), 5, 0.3D, 0.5D, 0.3D, 0.1D);
                Set<UUID> hitSet = DASH_HIT.get(id);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(1.2D),
                        e -> e != player && e.isAlive() && !e.isAlliedTo(player))) {
                    if (hitSet != null && hitSet.add(target.getUUID())) {
                        target.hurt(level.damageSources().playerAttack(player), DASH_DAMAGE);
                        target.knockback(DASH_KNOCKBACK,
                                player.getX() - target.getX(), player.getZ() - target.getZ());
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                target.getX(), target.getY() + 1.0D, target.getZ(), 1, 0, 0, 0, 0);
                        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);
                    }
                }
                DASHING.put(id, dashLeft - 1);
            }
        }

        // X 격노 만료
        Long frenzyUntil = FRENZY_UNTIL.get(id);
        if (frenzyUntil != null && now >= frenzyUntil) {
            endFrenzy(player);
            player.displayClientMessage(Component.literal("격노 종료"), true);
        }

        // 궁 광폭화: 지속 중 붉은 기운 + 만료
        Long rageUntil = RAGE_UNTIL.get(id);
        if (rageUntil != null) {
            if (now >= rageUntil) {
                RAGE_UNTIL.remove(id);
                RAGE_BONUS.remove(id);
                player.displayClientMessage(Component.literal("광폭화 종료"), true);
            } else if (now % 4 == 0) {
                level.sendParticles(BLOOD_DUST,
                        player.getX(), player.getY() + 1.1D, player.getZ(), 2, 0.35D, 0.5D, 0.35D, 0.0D);
                // 광폭화 지속 중에도 몸에서 작은 불꽃이 주기적으로 뿜어짐
                if (now % 8 == 0) {
                    burstRageFlames(level, player, 2, 0.15D);
                }
            }
        }

        // 분노 자연 감소: 전투 없이 5초 지나면 2초마다 1개씩
        int rage = RAGE_STACKS.getOrDefault(id, 0);
        if (rage > 0) {
            long idle = now - LAST_COMBAT.getOrDefault(id, now);
            if (idle >= RAGE_DECAY_DELAY && (idle - RAGE_DECAY_DELAY) % RAGE_DECAY_INTERVAL == 0) {
                setRage(player, rage - 1);
            }
        }
    }

    // 분노 불꽃을 몸 중심에서 사방으로 뿜어냄 (count=0 전송 = 오프셋이 속도 방향이 됨)
    private static void burstRageFlames(ServerLevel level, ServerPlayer player, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double yaw = player.getRandom().nextDouble() * Math.PI * 2;
            // 수평 위주 + 약간 위쪽으로 치우친 방사 방향
            double pitch = (player.getRandom().nextDouble() - 0.35) * Math.PI * 0.6;
            double dx = Math.cos(yaw) * Math.cos(pitch);
            double dz = Math.sin(yaw) * Math.cos(pitch);
            double dy = Math.sin(pitch) + 0.3;
            level.sendParticles(ModParticles.RAGE.get(),
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    0, dx, dy, dz, speed);
        }
    }

    public static boolean isRaging(ServerPlayer player) {
        Long until = RAGE_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    private static boolean isBerserker(ServerPlayer player) {
        return PlayerCharacterManager.getCharacter(player) == CharacterType.BERSERKER;
    }

    // X 사용 가능 여부 (SkillManager가 자원 소모 전에 확인)
    public static boolean hasFrenzyRage(ServerPlayer player) {
        return RAGE_STACKS.getOrDefault(player.getUUID(), 0) >= FRENZY_RAGE_COST;
    }

    // 분노 획득: 전투 시각 갱신 + 최대치 제한
    private static void addRage(ServerPlayer player, int amount) {
        LAST_COMBAT.put(player.getUUID(), player.level().getGameTime());
        int cur = RAGE_STACKS.getOrDefault(player.getUUID(), 0);
        setRage(player, Math.min(MAX_RAGE, cur + amount));
    }

    // 분노 변경 + 클라 HUD 동기화 (변화 없으면 패킷 생략)
    private static void setRage(ServerPlayer player, int stacks) {
        int cur = RAGE_STACKS.getOrDefault(player.getUUID(), 0);
        if (stacks == cur) {
            return;
        }
        RAGE_STACKS.put(player.getUUID(), stacks);
        ModMessages.sendToPlayer(new com.example.examplemod.network.RageS2CPacket(stacks), player);
    }

    // 로그아웃·리스폰 시 상태 정리
    public static void clear(ServerPlayer player) {
        DASHING.remove(player.getUUID());
        DASH_HIT.remove(player.getUUID());
        RAGE_UNTIL.remove(player.getUUID());
        RAGE_BONUS.remove(player.getUUID());
        LAST_COMBAT.remove(player.getUUID());
        if (RAGE_STACKS.remove(player.getUUID()) != null) {
            ModMessages.sendToPlayer(new com.example.examplemod.network.RageS2CPacket(0), player);
        }
        player.removeEffect(com.example.examplemod.registry.ModEffects.RAMPAGE.get());
        endFrenzy(player);
    }

    private static void endFrenzy(ServerPlayer player) {
        FRENZY_UNTIL.remove(player.getUUID());
        removeModifier(player, Attributes.ATTACK_SPEED, FRENZY_ATTACK_SPEED_UUID);
        removeModifier(player, Attributes.MOVEMENT_SPEED, FRENZY_MOVE_SPEED_UUID);
        player.removeEffect(com.example.examplemod.registry.ModEffects.FRENZY.get());
    }

    private static void addModifier(ServerPlayer player, Attribute attribute, UUID uuid,
                                    String name, double amount, AttributeModifier.Operation operation) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) {
            inst.removeModifier(uuid);   // 중복 시전 대비 — 있던 건 지우고 새로 적용
            inst.addTransientModifier(new AttributeModifier(uuid, name, amount, operation));
        }
    }

    private static void removeModifier(ServerPlayer player, Attribute attribute, UUID uuid) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) {
            inst.removeModifier(uuid);
        }
    }
}
