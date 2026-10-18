package com.example.examplemod.combat;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GamblerSkills implements CharacterSkills {

    // 궁 시전 가능 여부 (SkillManager가 게이지 소모 전에 확인 — 헛방으로 게이지가 날아가지 않게)
    public static boolean canStartRoulette(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        boolean busy = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(d -> d.getRouletteTicks() > 0)
                .orElse(false);
        return !busy && findLookTarget(player, level, 8.0) != null;
    }

    // 빈 코인 슬롯이 있는지 (SkillManager가 자원 소모 전에 확인)
    public static boolean hasEmptyCoinSlot(ServerPlayer player) {
        return player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(d -> d.getCoinType(0) == CoinType.NONE
                        || d.getCoinType(1) == CoinType.NONE
                        || d.getCoinType(2) == CoinType.NONE)
                .orElse(false);
    }

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick) {
        // Z: 코인 토스 — 7(공격 +15%) 또는 해골(공격 -15%), 50:50, 20초, 3중첩
        // (가득 참 체크는 SkillManager가 스태미나 소모 전에 함. 결과는 UI로만 표시)
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            int slot = -1;
            for (int i = 0; i < 3; i++) {
                if (data.getCoinType(i) == CoinType.NONE) {
                    slot = i;
                    break;
                }
            }
            if (slot == -1) {
                return;   // 안전장치 (정상 흐름에선 도달 안 함)
            }

            CoinType result = player.getRandom().nextBoolean() ? CoinType.SEVEN : CoinType.SKULL;
            data.setCoin(slot, result, GamblerEvents.COIN_TICKS);
            GamblerEvents.syncCoinEffects(player, data);   // 오른쪽 위 효과 아이콘 갱신
        });
        CombatSync.syncStats(player);   // HUD 코인 이미지 갱신

        // 동전 튕기는 소리 (주변에도 들림, 피치 약간 랜덤)
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                com.example.examplemod.registry.ModSounds.COIN_TOSS.get(),
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.8F, 0.95F + player.getRandom().nextFloat() * 0.1F);
    }

    @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        // X: 블랙잭 카드 던지기 — 맞은 적에게 카드 부착 (이후 총알 액면가가 누적, 21 = 폭발)
        CommonCombatActions.fireProjectile(player, 15,
                new ItemStack(ModItems.CARD.get()), 1.0F);   // 카드 자체 데미지 1
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // 궁극기 "룰렛": 바라보는 상대와 함께 블럭 돔에 갇혀 슬롯머신 3릴 결과를 지켜봄
        //   7 2개 이상 = 나 공격력 +40% (30초) / 해골 2개 이상 = 상대 공격력 -50% (30초)
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // 이미 룰렛 진행 중이면 무시
        boolean busy = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(d -> d.getRouletteTicks() > 0)
                .orElse(false);
        if (busy) {
            return;
        }

        LivingEntity target = findLookTarget(player, level, 8.0);
        if (target == null) {
            // SkillManager가 미리 막지만 만약을 위해 — 게이지 전액 환불
            player.sendSystemMessage(Component.literal("대상이 없습니다."));
            UltimateGaugeManager.addGauge(player, UltimateGaugeManager.MAX_GAUGE);
            return;
        }

        GamblerEvents.startRoulette(player, level, target);
    }

    // 시선 방향의 가장 가까운 생물 찾기 (도적 Z와 같은 방식)
    private static LivingEntity findLookTarget(ServerPlayer player, ServerLevel level, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB box = new AABB(eye, eye.add(look.scale(range))).inflate(1.0);

        LivingEntity target = null;
        double bestProj = range;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> x != player && x.isAlive() && !x.isAlliedTo(player))) {   // 같은 팀은 룰렛 대상 제외
            Vec3 c = e.getBoundingBox().getCenter();
            double proj = c.subtract(eye).dot(look);
            if (proj < 0 || proj > range) {
                continue;
            }
            if (eye.add(look.scale(proj)).distanceTo(c) <= 1.0 && proj < bestProj) {
                bestProj = proj;
                target = e;
            }
        }
        return target;
    }
}
