package com.example.examplemod.combat;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import net.minecraft.server.level.ServerPlayer;

public class HammerSkills implements CharacterSkills{

    public static final int THROW_STUN_TICKS = 40;   // 투척 명중 시 스턴 (2초)

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick ){
        // Z: 해머 투척 — 명중 시 스턴 (스턴 판정은 SkillProjectileEntity, 세로 회전은 SkillProjectileRenderer)
        // 날아가는 모습은 전용 모델(hammer_thrown.json)의 ground 항목으로 조절
        // 사거리 무제한 — 땅에 닿거나 수명(5초)이 다하면 소멸
        CommonCombatActions.fireProjectile(player, CommonCombatActions.UNLIMITED_RANGE,
                new net.minecraft.world.item.ItemStack(com.example.examplemod.registry.ModItems.HAMMER_THROWN.get()), 2.0F);
    }

     @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        // 토글 종료·망치 확인은 SkillManager 사전 체크에서 끝남 — 여기 도달 = 회전 시작
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .ifPresent(data -> HammerSpinEvents.startSpin(player, data));
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // 궁 "대지 균열": 시전 모션 1.5초 + 0.5초 뒤(내려찍는 순간) 부채꼴 균열 발동
        com.example.examplemod.network.ModMessages.sendToTrackingAndSelf(
                new com.example.examplemod.network.PlayAnimationS2CPacket(player.getId(), "hammer_ultimate"), player);
        HammerUltimateEvents.schedule(player);
    }
}
