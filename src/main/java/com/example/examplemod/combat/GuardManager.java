package com.example.examplemod.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GuardManager {

    private static final Set<UUID> GUARDING_PLAYERS = new HashSet<>();

    private static final UUID GUARD_SPEED_MODIFIER_UUID =
            UUID.fromString("7c8f2d3a-5b6e-4b8d-9c1a-333333333333");

    public static void setGuarding(ServerPlayer player, boolean guarding) {
        if (guarding) {
            GUARDING_PLAYERS.add(player.getUUID());
            applySlow(player);
        } else {
            GUARDING_PLAYERS.remove(player.getUUID());
            removeSlow(player);
        }
    }

    public static boolean isGuarding(ServerPlayer player) {
        return GUARDING_PLAYERS.contains(player.getUUID());
    }

    public static void clear(ServerPlayer player) {
    GUARDING_PLAYERS.remove(player.getUUID());
}


    private static void applySlow(ServerPlayer player) {
        var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movementSpeed == null) {
            return;
        }

        movementSpeed.removeModifier(GUARD_SPEED_MODIFIER_UUID);

        AttributeModifier modifier = new AttributeModifier(
                GUARD_SPEED_MODIFIER_UUID,
                "Guard slow",
                -0.09D,
                AttributeModifier.Operation.ADDITION
        );

        movementSpeed.addTransientModifier(modifier);
    }

    private static void removeSlow(ServerPlayer player) {
        var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movementSpeed == null) {
            return;
        }

        movementSpeed.removeModifier(GUARD_SPEED_MODIFIER_UUID);
    }
}