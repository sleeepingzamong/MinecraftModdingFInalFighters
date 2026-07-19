package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 도적 X 스킬: 표창 명중 → 표식(3초, 대상 머리 위 단검 표시) → X 재입력 시 추적 돌진 + 타격
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class DojukMarkEvents {

    public static final int MARK_TICKS = 60;        // 표식 유지 3초
    public static final int DASH_TICKS = 200;       // 돌진 최대 10초 (안전장치 — 보통은 도달하면 끝)
    private static final double DASH_SPEED = 1.5;   // 틱당 이동량(칸)
    private static final double HIT_RANGE = 2.0;    // 이 거리 안이면 도달로 판정
    private static final float DASH_DAMAGE = 6.0F;  // 도달 시 데미지 (체력 3칸)

    public static final int INVIS_TICKS = 200;            // 궁극기 투명화 10초
    private static final int AFTERIMAGE_PERIOD = 40;      // 잔상 주기 2초
    private static final float INVIS_BONUS_DAMAGE = 2.0F; // 투명 중 공격 추가데미지 (하트 1개)

    // 표창 명중 시 호출: 표식 기록 + 대상 머리 위에 단검 모델 띄우기
    public static void applyMark(ServerPlayer shooter, LivingEntity target) {
        if (!(shooter.level() instanceof ServerLevel level)) {
            return;
        }

        // 이전 표식 표시가 남아 있으면 제거
        shooter.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            Entity old = level.getEntity(data.getDojukMarkDisplayId());
            if (old != null) {
                old.discard();
            }
        });

        // 머리 위 단검(아이템 디스플레이) 생성 — setItemStack이 외부에서 못 부르는 메서드라 /summon처럼 NBT로 만든다
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:item_display");
        tag.put("item", new ItemStack(ModItems.KNIFE.get()).save(new CompoundTag()));
        tag.putString("billboard", "center");   // 항상 카메라를 향함
        Entity display = EntityType.loadEntityRecursive(tag, level, e -> {
            e.setPos(target.getX(), target.getY() + target.getBbHeight() + 0.7, target.getZ());
            return e;
        });
        int displayId = -1;
        if (display != null) {
            level.addFreshEntity(display);
            displayId = display.getId();
        }

        final int finalDisplayId = displayId;
        shooter.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            data.setDojukMarkTargetId(target.getId());
            data.setDojukMarkTicks(MARK_TICKS);
            data.setDojukMarkDisplayId(finalDisplayId);
        });
    }

    // PlayerTickHub가 매 틱 호출 (이미 조회한 data를 받아 재조회 없음)
    public static void tick(ServerPlayer player, ServerLevel level, PlayerCombatData data) {
        {
            // 1) 표식 카운트다운 + 머리 위 단검 위치 갱신
            int mark = data.getDojukMarkTicks();
            if (mark > 0) {
                Entity target = level.getEntity(data.getDojukMarkTargetId());
                Entity display = level.getEntity(data.getDojukMarkDisplayId());

                if (target == null || !target.isAlive()) {
                    // 대상이 죽거나 사라짐 → 표식 정리
                    if (display != null) {
                        display.discard();
                    }
                    data.setDojukMarkTicks(0);
                    data.setDojukMarkTargetId(-1);
                    data.setDojukMarkDisplayId(-1);
                } else {
                    if (display != null) {
                        display.setPos(target.getX(), target.getY() + target.getBbHeight() + 0.7, target.getZ());
                    }
                    data.setDojukMarkTicks(mark - 1);
                    if (mark - 1 <= 0) {
                        // 시간 만료 → 표식 소멸
                        if (display != null) {
                            display.discard();
                        }
                        data.setDojukMarkTargetId(-1);
                        data.setDojukMarkDisplayId(-1);
                    }
                }
            }

            // 2) 궁극기 투명화: 카운트다운 + 2초마다 잔상 파티클
            int invis = data.getDojukInvisTicks();
            if (invis > 0) {
                int left = invis - 1;
                data.setDojukInvisTicks(left);
                if (left > 0 && left % AFTERIMAGE_PERIOD == 0) {
                    // 잔상: 지나간 자리에 사람 크기 연기 실루엣 (모두에게 보임)
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            30, 0.25, 0.7, 0.25, 0.01);
                }
            }

            // 3) 돌진: 매 틱 대상 쪽으로 가속(추적), 닿으면 타격
            int dash = data.getDojukDashTicks();
            if (dash > 0) {
                Entity target = level.getEntity(data.getDojukMarkTargetId());
                if (target == null || !target.isAlive()) {
                    data.setDojukDashTicks(0);
                    data.setDojukMarkTargetId(-1);
                    return;
                }

                Vec3 myCenter = player.position().add(0, player.getBbHeight() / 2.0, 0);
                Vec3 to = target.getBoundingBox().getCenter().subtract(myCenter);
                double dist = to.length();

                if (dist <= HIT_RANGE) {
                    // 도달 → 체력 3칸 타격, 돌진 종료
                    if (target instanceof LivingEntity living) {
                        living.hurt(player.damageSources().playerAttack(player), DASH_DAMAGE);
                    }
                    player.setDeltaMovement(player.getDeltaMovement().scale(0.2));
                    player.hurtMarked = true;
                    data.setDojukDashTicks(0);
                    data.setDojukMarkTargetId(-1);
                } else {
                    player.setDeltaMovement(to.normalize().scale(DASH_SPEED));
                    player.hurtMarked = true;
                    data.setDojukDashTicks(dash - 1);
                    if (dash - 1 <= 0) {
                        data.setDojukMarkTargetId(-1);   // 시간 내 못 닿음 → 종료
                    }
                }
            }
        }
    }

    // 투명 상태에서 공격하면: +2 데미지를 얹고 투명화 해제
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        attacker.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            if (data.getDojukInvisTicks() > 0) {
                event.setAmount(event.getAmount() + INVIS_BONUS_DAMAGE);
                data.setDojukInvisTicks(0);
                attacker.removeEffect(MobEffects.INVISIBILITY);   // 공격 → 투명 풀림
            }
        });
    }
}
