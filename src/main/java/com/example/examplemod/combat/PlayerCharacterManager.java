package  com.example.examplemod.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;


import com.example.examplemod.capability.PlayerCombatDataProvider;
import java.util.UUID;

import com.example.examplemod.capability.PlayerCombatDataProvider;

public class PlayerCharacterManager {

    private static final UUID CHARACTER_HEALTH_MODIFIER_UUID =
        UUID.fromString("9f0f5c8a-4b8f-4f47-9a9b-222222222222");

    public static void setCharacter(Player player, CharacterType characterType) {
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
               .ifPresent(data -> data.setCharacter(characterType));
        applyCharacterHealth(player, characterType);

         if (player instanceof ServerPlayer serverPlayer) {
        StaminaManager.resetStamina(serverPlayer, characterType);
    }
    }

    public static CharacterType getCharacter(Player player) {
        return player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                      .map(data -> data.getCharacter())
                      .orElse(CharacterType.NONE);
    }

    public static void reapplyCharacterHealth(Player player) {
         applyCharacterHealth(player, getCharacter(player));
     }

    private static void applyCharacterHealth(Player player, CharacterType characterType) {
    AttributeInstance maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH);

    if (maxHealthAttribute == null) {
        return;
    }

    maxHealthAttribute.removeModifier(CHARACTER_HEALTH_MODIFIER_UUID);

    double baseHealth = 20.0D;
    double characterMaxHealth = CharacterStats.getMaxHealth(characterType);
    double bonusHealth = characterMaxHealth - baseHealth;

    if (bonusHealth != 0) {
        AttributeModifier modifier = new AttributeModifier(
                CHARACTER_HEALTH_MODIFIER_UUID,
                "Character max health bonus",
                bonusHealth,
                AttributeModifier.Operation.ADDITION
        );

        maxHealthAttribute.addPermanentModifier(modifier);
    }

    player.setHealth((float) player.getMaxHealth());
    }
}