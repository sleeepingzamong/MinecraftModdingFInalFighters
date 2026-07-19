package com.example.examplemod.combat;

import net.minecraft.server.level.ServerPlayer;

public class SamuraiSkills implements CharacterSkills{

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick ){
        
    }

     @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // player.sendSystemMessage(Component.literal("사이보그 궁극기 사용! 차징: " + chargeTick + "틱"));
    }
}
