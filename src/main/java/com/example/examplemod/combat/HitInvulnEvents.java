package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// 피격 후 무적시간 단축: 바닐라 0.5초 → 0.1초 — 단, "엔티티 공격"에 한해서만.
// 용암·불·선인장·낙하 같은 환경 피해는 바닐라 0.5초 무적을 유지한다 (안 그러면 용암이 초당 20씩 갈아버림).
// 바닐라는 맞으면 invulnerableTime=20이 되고 그중 10틱 초과 구간(=0.5초)이 실제 무적 —
// 공격으로 맞은 직후에만 12로 잘라내 실제 무적을 2틱(0.1초)으로 줄인다.
// (훈련용 좀비 등 몹에도 동일 적용 — 더미로 잰 DPS가 실전과 같게)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class HitInvulnEvents {

    private static final int INVULN_CAP = 12;   // 10 + 실제 무적 틱 수 (12 = 0.1초, 바닐라 20 = 0.5초)

    // 마지막 피해가 "엔티티 공격"이었던 대상 — 다음 틱에 무적을 잘라낼 목록
    private static final Set<UUID> ATTACKED = new HashSet<>();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        // 때린 주체(플레이어·몹·투사체 주인)가 있으면 공격, 없으면 환경 피해
        if (event.getSource().getEntity() != null) {
            ATTACKED.add(event.getEntity().getUUID());
        } else {
            ATTACKED.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity.invulnerableTime > INVULN_CAP) {
            if (ATTACKED.contains(entity.getUUID())) {
                entity.invulnerableTime = INVULN_CAP;   // 공격 피격만 0.1초로 단축
            }
            // 환경 피해는 바닐라 0.5초 그대로
        } else if (entity.invulnerableTime == 0) {
            ATTACKED.remove(entity.getUUID());          // 무적 끝 → 기록 정리
        }
    }
}
