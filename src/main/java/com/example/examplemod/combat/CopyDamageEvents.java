package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capability.PlayerCombatDataProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 복사 스킬 시전 중(근접 타격 포함)이면 가해 데미지 +10%.
// 투사체 데미지는 발사 시점에 이미 ×1.1 되어 있으므로(그때는 버프 ON),
// 투사체가 나중에 맞을 땐 버프가 꺼져 있어서 여기서 중복 적용되지 않는다.
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class CopyDamageEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        boolean buff = attacker.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(PlayerCombatData::isCopyBuffActive).orElse(false);
        if (buff) {
            event.setAmount(event.getAmount() * 1.1F);
        }
    }
}
