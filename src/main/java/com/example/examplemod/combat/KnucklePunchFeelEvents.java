package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.PowerKnuckleItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 너클 타격감: 주먹이 꽂힐 때 임팩트 파티클 + 묵직한 타격음 + 살짝 밀림
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class KnucklePunchFeelEvents {

    private static final float PUNCH_KNOCKBACK = 0.15F;   // 타격당 미세 넉백 (누적되면 밀리는 느낌)

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 플레이어의 "직접 타격"만 (투사체·스킬 광역은 제외)
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || event.getSource().getDirectEntity() != attacker) {
            return;
        }
        if (!(attacker.getMainHandItem().getItem() instanceof PowerKnuckleItem)) {
            return;
        }
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target = event.getEntity();
        double ix = target.getX();
        double iy = target.getY() + target.getBbHeight() * 0.6D;   // 가슴 높이
        double iz = target.getZ();

        // 임팩트: 크리트 튐 + 기계 주먹 스파크 + 먼지 퍽
        level.sendParticles(ParticleTypes.CRIT, ix, iy, iz, 8, 0.2D, 0.2D, 0.2D, 0.3D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, ix, iy, iz, 4, 0.15D, 0.15D, 0.15D, 0.2D);
        level.sendParticles(ParticleTypes.POOF, ix, iy, iz, 2, 0.1D, 0.1D, 0.1D, 0.02D);

        // 묵직한 "퍽" — 넉백 타격음 + 아주 작은 금속 울림 (기계 주먹)
        level.playSound(null, ix, iy, iz,
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.85F);
        level.playSound(null, ix, iy, iz,
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.12F, 2.0F);

        // 미세 넉백 — 연타가 꽂힐수록 상대가 조금씩 밀림
        Vec3 dir = target.position().subtract(attacker.position()).normalize();
        target.push(dir.x * PUNCH_KNOCKBACK, 0.04D, dir.z * PUNCH_KNOCKBACK);
    }
}
