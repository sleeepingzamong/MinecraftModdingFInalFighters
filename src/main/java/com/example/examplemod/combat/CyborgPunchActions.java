package com.example.examplemod.combat;

import com.example.examplemod.item.PowerKnuckleItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 사이보그 너클 전용 조작: 좌클릭 = 왼손 펀치, 우클릭 = 오른손 펀치
// (클라이언트 CyborgPunchEvents가 입력을 가로채 PunchC2SPacket으로 보냄)
public class CyborgPunchActions {

    // ==================== 튜닝 상수 ====================
    private static final float LEFT_PUNCH_DAMAGE = 3.0F;   // 왼손 잽 데미지 (오른손은 너클 스탯 6 그대로)
    private static final int LEFT_COOLDOWN_TICKS = 6;      // 왼손 쿨 0.3초 — 빠른 잽
    private static final int RIGHT_COOLDOWN_TICKS = 20;    // 오른손 쿨 1초 — 느린 스트레이트 (공속 1.0과 일치)
    // =================================================

    private static final Map<UUID, Long> LEFT_READY = new HashMap<>();    // 왼손 쿨
    private static final Map<UUID, Long> RIGHT_READY = new HashMap<>();   // 오른손 쿨

    public static void punch(ServerPlayer player, boolean leftHand) {
        // 검증: 사이보그 너클을 들고 있어야 함 (치트 패킷 방지)
        if (!(player.getMainHandItem().getItem() instanceof PowerKnuckleItem)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        long now = level.getGameTime();
        Map<UUID, Long> cooldown = leftHand ? LEFT_READY : RIGHT_READY;
        if (now < cooldown.getOrDefault(player.getUUID(), 0L)) {
            return;   // 아직 쿨타임
        }
        cooldown.put(player.getUUID(), now + (leftHand ? LEFT_COOLDOWN_TICKS : RIGHT_COOLDOWN_TICKS));

        // 팔 스윙 (모두에게 보임)
        player.swing(leftHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, true);

        // 시선 방향으로 사거리 안의 생명체 찾기 (너클의 -0.5 사거리 반영)
        double reach = player.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        LivingEntity target = findTarget(player, reach);

        if (target == null) {
            // 허공 휘두름
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.7F, leftHand ? 1.1F : 0.9F);
            return;
        }

        target.invulnerableTime = 0;   // 양손 교대 연타가 무적시간에 먹히지 않게

        if (leftHand) {
            // 왼손: 커스텀 타격 (고정 데미지)
            target.hurt(level.damageSources().playerAttack(player), LEFT_PUNCH_DAMAGE);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.1F);
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1.0D, target.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.1D);
        } else {
            // 오른손: 커스텀 스트레이트 — 바닐라 공속 게이지와 무관하게 항상 풀데미지
            // (연타 제한은 위의 RIGHT_COOLDOWN_TICKS가 담당. 잽 직후에도 안 약해짐)
            float damage = (float) player.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            target.hurt(level.damageSources().playerAttack(player), damage);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.9F);
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1.0D, target.getZ(), 5, 0.25D, 0.25D, 0.25D, 0.15D);
        }

        // 도박사 카드 복사 시너지: 내가 붙인 카드가 이 대상에게 있으면 펀치 액면가가 21에 누적
        // (잽 = 3 고정, 스트레이트 = 너클 공격력. 정확히 21 = 폭발, 넘으면 버스트 — 조합을 계산해서 쳐야 함)
        int faceValue = leftHand ? (int) LEFT_PUNCH_DAMAGE
                : (int) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        GamblerEvents.onBulletHit(player, target, faceValue);
    }

    // 눈 위치에서 바라보는 방향으로 reach 안의 가장 가까운 생명체
    private static LivingEntity findTarget(ServerPlayer player, double reach) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        Vec3 end = start.add(dir.scale(reach));
        AABB searchBox = player.getBoundingBox().expandTowards(dir.scale(reach)).inflate(1.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, searchBox,
                e -> e instanceof LivingEntity && e != player && e.isAlive() && e.isPickable(),
                reach * reach);

        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
}
