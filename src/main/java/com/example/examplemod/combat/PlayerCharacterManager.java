package  com.example.examplemod.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;


import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class PlayerCharacterManager {

    private static final UUID CHARACTER_HEALTH_MODIFIER_UUID =
        UUID.fromString("9f0f5c8a-4b8f-4f47-9a9b-222222222222");

    public static void setCharacter(Player player, CharacterType characterType) {
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
               .ifPresent(data -> data.setCharacter(characterType));
        applyCharacterHealth(player, characterType);

         if (player instanceof ServerPlayer serverPlayer) {
        StaminaManager.resetStamina(serverPlayer, characterType);
        giveCharacterWeapon(serverPlayer, characterType);
    }
    }

    // 캐릭터별 전용 무기. 무기가 없는 캐릭터(사이보그·해커·가수 등)는 null.
    // 캐릭터 무기 추가 시 여기 한 줄만 넣으면 선택 시 지급/교체에 자동 반영됨.
    private static Item getWeapon(CharacterType characterType) {
        return switch (characterType) {
            case MONKEY -> ModItems.RUYI_JINGU_BANG.get();
            case DOJUK -> ModItems.KNIFE.get();
            case GAMBLER -> ModItems.REVOLVER.get();
            case SAMURAI -> ModItems.KATANA.get();
            case HAMMER -> ModItems.HAMMER.get();
            default -> null;
        };
    }

    private static void giveCharacterWeapon(ServerPlayer player, CharacterType characterType) {
        // 1) 이전 캐릭터의 무기를 인벤토리에서 전부 제거 (캐릭터 변경 시 중복 방지)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isCharacterWeapon(stack.getItem())) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        // 2) 새 캐릭터의 무기 지급 (인벤토리가 꽉 찼으면 발밑에 드롭)
        Item weapon = getWeapon(characterType);
        if (weapon != null) {
            ItemStack weaponStack = new ItemStack(weapon);
            if (!player.getInventory().add(weaponStack)) {
                player.drop(weaponStack, false);
            }
        }
    }

    private static boolean isCharacterWeapon(Item item) {
        for (CharacterType type : CharacterType.values()) {
            if (item == getWeapon(type)) {
                return true;
            }
        }
        return false;
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