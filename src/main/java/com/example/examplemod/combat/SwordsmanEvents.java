package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.KatanaItem;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.ParryMarksS2CPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 검사 스킬 처리
// - Z 연격: 스킬 사용 후 평타 2회 동안, 명중하면 잠깐 뒤 같은 대상을 한 번 더 타격
// - X 패링: 머리 위 표식 3개 — 엔티티의 공격을 1회씩 완전 무효 (낙하·용암 등 환경 피해는 안 막음)
// - 궁 강화: 공격력·이동속도·공격속도 버프 + 가한 데미지 일부 흡혈
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class SwordsmanEvents {

    // ==================== 튜닝 상수 ====================
    private static final int ECHO_ATTACKS = 2;            // Z: 강화되는 평타 횟수
    private static final int ECHO_DELAY_TICKS = 3;        // 추가 타격까지의 간격 (0.15초)
    private static final float ECHO_DAMAGE_RATIO = 1.0F;  // 추가 타격 데미지 배율 (1.0 = 원래 평타와 동일)
    private static final double ECHO_MAX_RANGE = 6.0D;    // 추가 타격 유효 거리 (그 사이 도망가면 안 맞음)

    private static final int PARRY_MARKS = 3;             // X: 패링 표식 개수

    private static final int ENHANCE_DURATION_TICKS = 600;    // 궁 지속 (30초)
    private static final float ENHANCE_LIFESTEAL = 0.3F;      // 흡혈: 가한 데미지의 30% 회복
    private static final double ENHANCE_ATTACK_DAMAGE = 3.0D; // 공격력 +3
    private static final double ENHANCE_MOVE_SPEED = 0.2D;    // 이동속도 +20%
    private static final double ENHANCE_ATTACK_SPEED = 0.3D;  // 공격속도 +30%
    // =================================================

    private static final UUID ENHANCE_ATTACK_DAMAGE_UUID =
            UUID.fromString("6a1c9e2b-7d4f-4c3a-8b5e-444444444441");
    private static final UUID ENHANCE_MOVE_SPEED_UUID =
            UUID.fromString("6a1c9e2b-7d4f-4c3a-8b5e-444444444442");
    private static final UUID ENHANCE_ATTACK_SPEED_UUID =
            UUID.fromString("6a1c9e2b-7d4f-4c3a-8b5e-444444444444");

    private static final Map<UUID, Integer> ECHO_CHARGES = new HashMap<>();   // 연격 남은 평타 수
    private static final Map<UUID, Integer> PARRY = new HashMap<>();          // 남은 패링 표식
    private static final Map<UUID, Long> ENHANCE_UNTIL = new HashMap<>();     // 강화 종료 시각
    private static final List<EchoHit> PENDING_ECHO = new ArrayList<>();      // 예약된 추가 타격

    private static boolean echoInProgress = false;   // 추가 타격이 또 추가 타격을 만들지 않게

    private record EchoHit(UUID attacker, UUID victim, float damage, long atTick) {}

    // ===== Z 연격 =====
    public static void startEcho(ServerPlayer player) {
        ECHO_CHARGES.put(player.getUUID(), ECHO_ATTACKS);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.5F);
        player.displayClientMessage(Component.literal("연격 준비 — 다음 평타 " + ECHO_ATTACKS + "회 강화"), true);
    }

    // ===== X 패링 =====
    public static void startParry(ServerPlayer player) {
        PARRY.put(player.getUUID(), PARRY_MARKS);
        syncParry(player, PARRY_MARKS);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.literal("패링 태세 — 표식 " + PARRY_MARKS + "개"), true);
    }

    // ===== 궁 강화 =====
    public static void startEnhance(ServerPlayer player) {
        long now = player.level().getGameTime();
        ENHANCE_UNTIL.put(player.getUUID(), now + ENHANCE_DURATION_TICKS);

        // 물약 효과 대신 능력치를 직접 올림 (HUD에 물약 아이콘·소용돌이 파티클 안 생김)
        addModifier(player, Attributes.ATTACK_DAMAGE, ENHANCE_ATTACK_DAMAGE_UUID,
                "Swordsman enhance attack damage", ENHANCE_ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION);
        addModifier(player, Attributes.MOVEMENT_SPEED, ENHANCE_MOVE_SPEED_UUID,
                "Swordsman enhance move speed", ENHANCE_MOVE_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addModifier(player, Attributes.ATTACK_SPEED, ENHANCE_ATTACK_SPEED_UUID,
                "Swordsman enhance attack speed", ENHANCE_ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.5D, 0.8D, 0.5D, 0.05D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.3F);
        player.displayClientMessage(Component.literal("강화 발동! (" + (ENHANCE_DURATION_TICKS / 20) + "초)"), true);
    }

    public static boolean isEnhanced(ServerPlayer player) {
        Long until = ENHANCE_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    // 패링: 데미지 계산이 시작되기 전에 공격 자체를 무효화
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        Integer marks = PARRY.get(victim.getUUID());
        if (marks == null || marks <= 0) {
            return;
        }
        if (event.getSource().getEntity() == null) {
            return;   // 낙하·용암 등 환경 피해는 칼로 못 쳐냄
        }

        event.setCanceled(true);
        int left = marks - 1;
        if (left <= 0) {
            PARRY.remove(victim.getUUID());
        } else {
            PARRY.put(victim.getUUID(), left);
        }
        syncParry(victim, left);

        // 금속 "챙!" + 불꽃 튀는 연출
        ServerLevel level = (ServerLevel) victim.level();
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 2.0F);
        level.sendParticles(ParticleTypes.CRIT,
                victim.getX(), victim.getY() + 1.2D, victim.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.2D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                victim.getX(), victim.getY() + 1.2D, victim.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.3D);
        victim.displayClientMessage(Component.literal("패링! 남은 표식 " + left + "개"), true);
    }

    // 연격 추가 타격 예약 + 궁 흡혈
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        // 궁 강화 중이면 흡혈 (평타·스킬·추가 타격 전부)
        if (isEnhanced(attacker)) {
            attacker.heal(event.getAmount() * ENHANCE_LIFESTEAL);
        }

        if (echoInProgress) {
            return;   // 추가 타격 자신은 또 예약하지 않음
        }
        if (event.getSource().getDirectEntity() != attacker) {
            return;   // 투사체·간접 피해는 연격 대상 아님 (직접 근접 공격만)
        }
        if (!(attacker.getMainHandItem().getItem() instanceof KatanaItem)) {
            return;   // 일본도로 친 평타만
        }
        Integer charges = ECHO_CHARGES.get(attacker.getUUID());
        if (charges == null || charges <= 0) {
            return;
        }

        int left = charges - 1;
        if (left <= 0) {
            ECHO_CHARGES.remove(attacker.getUUID());
        } else {
            ECHO_CHARGES.put(attacker.getUUID(), left);
        }
        PENDING_ECHO.add(new EchoHit(attacker.getUUID(), event.getEntity().getUUID(),
                event.getAmount() * ECHO_DAMAGE_RATIO,
                attacker.level().getGameTime() + ECHO_DELAY_TICKS));
    }

    // PlayerTickHub가 매 틱 호출
    public static void tick(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();

        // 예약된 추가 타격 실행 (이 플레이어가 시전자인 것만)
        if (!PENDING_ECHO.isEmpty()) {
            Iterator<EchoHit> it = PENDING_ECHO.iterator();
            while (it.hasNext()) {
                EchoHit hit = it.next();
                if (!hit.attacker().equals(player.getUUID()) || now < hit.atTick()) {
                    continue;
                }
                it.remove();
                Entity target = level.getEntity(hit.victim());
                if (!(target instanceof LivingEntity living) || !living.isAlive()
                        || living.distanceTo(player) > ECHO_MAX_RANGE) {
                    continue;
                }
                echoInProgress = true;
                try {
                    living.invulnerableTime = 0;   // 방금 맞은 무적시간을 무시하고 한 번 더
                    living.hurt(level.damageSources().playerAttack(player), hit.damage());
                } finally {
                    echoInProgress = false;
                }
                level.playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.1F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        living.getX(), living.getY() + 1.0D, living.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        // 궁 강화: 만료 처리 + 지속 연출
        Long until = ENHANCE_UNTIL.get(player.getUUID());
        if (until != null) {
            if (now >= until || !player.isAlive()) {
                endEnhance(player);
            } else if (now % 5 == 0) {
                level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0.3D, 0.5D, 0.3D, 0.0D);
            }
        }
    }

    private static void endEnhance(ServerPlayer player) {
        ENHANCE_UNTIL.remove(player.getUUID());
        removeModifier(player, Attributes.ATTACK_DAMAGE, ENHANCE_ATTACK_DAMAGE_UUID);
        removeModifier(player, Attributes.MOVEMENT_SPEED, ENHANCE_MOVE_SPEED_UUID);
        removeModifier(player, Attributes.ATTACK_SPEED, ENHANCE_ATTACK_SPEED_UUID);
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

    // 사망·리스폰·접속종료 시 상태 정리
    public static void clear(ServerPlayer player) {
        ECHO_CHARGES.remove(player.getUUID());
        if (PARRY.remove(player.getUUID()) != null) {
            syncParry(player, 0);
        }
        endEnhance(player);
        PENDING_ECHO.removeIf(hit -> hit.attacker().equals(player.getUUID()));
    }

    private static void syncParry(ServerPlayer player, int marks) {
        ModMessages.sendToTrackingAndSelf(new ParryMarksS2CPacket(player.getId(), marks), player);
    }
}
