package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.config.FFConfig;
import com.example.examplemod.network.DamagePopupS2CPacket;
import com.example.examplemod.network.ModMessages;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 데미지·회복 숫자 팝업 (서버 측): 주변 클라이언트에 팝업 패킷 전송.
// 실제 그리기는 ClientDamagePopups가 지형 단계 깊이 판정으로 — 블록엔 가려지고 엔티티엔 안 가려짐.
// 색: 기본 피해 금색 / 갬블러 총알 헤드샷 빨강 / 회복 초록 "+숫자"
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class FloatingDamageNumbers {

    // 총알 헤드샷 표시용 스위치 — hurt 호출 직전에 켜고 직후에 끔 (서버 스레드 순차 실행이라 안전)
    public static boolean bulletHeadshot = false;

    // 자연 회복 스위치 — HealthRegenEvents가 heal 직전에 켜고 직후에 끔.
    // 0.5초마다 0.5씩 들어오는 자연 회복까지 띄우면 숫자가 쏟아지므로, 스킬·효과 회복만 표시
    public static boolean passiveRegen = false;

    // 모든 피해에 숫자 팝업 (최종 적용 데미지 기준 — 방어구·가드 감소 반영된 값)
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getAmount() <= 0 || victim.level().isClientSide()) {
            return;
        }
        send(victim, event.getAmount(), bulletHeadshot ? DamagePopupS2CPacket.HEADSHOT : DamagePopupS2CPacket.DAMAGE);
    }

    // 회복 숫자: 다른 핸들러가 양을 조정한 뒤의 최종값 기준, 체력이 이미 꽉 차서 실제로 안 오른 만큼은 빼고 표시
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (passiveRegen || entity.level().isClientSide() || !entity.isAlive()) {
            return;
        }
        float healed = Math.min(event.getAmount(), entity.getMaxHealth() - entity.getHealth());
        if (healed < 0.05F) {
            return;
        }
        send(entity, healed, DamagePopupS2CPacket.HEAL);
    }

    private static void send(LivingEntity entity, float amount, byte kind) {
        // 몸 주변 랜덤 위치: 수평은 몸 중심 둘레 0.45~0.9칸, 수직은 몸통 높이의 25~110% 구간
        // (config의 height는 전체를 위/아래로 미는 보정값 — 몸 주변 랜덤이 기본이면 0.0 권장)
        double angle = entity.getRandom().nextDouble() * Math.PI * 2;
        double radius = 0.45 + entity.getRandom().nextDouble() * 0.45;
        double x = entity.getX() + Math.cos(angle) * radius;
        double y = entity.getY() + entity.getBbHeight() * (0.25 + entity.getRandom().nextDouble() * 0.85)
                + FFConfig.POPUP_HEIGHT.get();
        double z = entity.getZ() + Math.sin(angle) * radius;
        ModMessages.sendToTracking(new DamagePopupS2CPacket(x, y, z, amount, kind), entity);
    }
}
