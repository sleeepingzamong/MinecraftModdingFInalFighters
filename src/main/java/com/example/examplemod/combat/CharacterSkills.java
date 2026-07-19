package com.example.examplemod.combat;

import net.minecraft.server.level.ServerPlayer;

public interface CharacterSkills {

    void useSkill1(ServerPlayer player, int chargeTick);

    void useSkill2(ServerPlayer player, int chargeTick);

    void useUltimate(ServerPlayer player, int chargeTick);
}