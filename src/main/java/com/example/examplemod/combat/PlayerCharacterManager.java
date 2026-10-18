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
        giveCharacterArmor(serverPlayer, characterType);

        // 해커: 선택 직후 롤백(Z) 못 쓰게 쿨타임부터 시작 (위치 기록이 쌓일 시간)
        if (characterType == CharacterType.HACKER) {
            SkillManager.applyCooldown(serverPlayer, SkillType.SKILL_1,
                    CharacterStats.getSkillCooldown(CharacterType.HACKER, SkillType.SKILL_1));
        }
    }
    }

    // 캐릭터별 전용 무기. 무기가 없는 캐릭터(사이보그·해커·가수 등)는 null.
    // 캐릭터 무기 추가 시 여기 한 줄만 넣으면 선택 시 지급/교체에 자동 반영됨.
    private static Item getWeapon(CharacterType characterType) {
        return switch (characterType) {
            case MONKEY -> ModItems.RUYI_JINGU_BANG.get();
            case DOJUK -> ModItems.KNIFE.get();
            case GAMBLER -> ModItems.REVOLVER.get();
            case SWORDSMAN -> ModItems.KATANA.get();
            case HAMMER -> ModItems.HAMMER.get();
            case CYBORG -> ModItems.POWER_KNUCKLE.get();
            case HACKER -> ModItems.CYBER_SCYTHE.get();
            case BERSERKER -> ModItems.BATTLE_AXE.get();
            case PHOENIX -> ModItems.FEATHER_FAN.get();
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

            // 사이보그는 양손 — 왼손(오프핸드)에도 너클 장착
            // (공격 스탯은 오른손에만 적용되므로 데미지가 2배가 되진 않음)
            if (characterType == CharacterType.CYBORG) {
                ItemStack prevOffhand = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                if (!prevOffhand.isEmpty() && !isCharacterWeapon(prevOffhand.getItem())) {
                    // 원래 왼손에 있던 일반 아이템은 인벤토리로 돌려줌
                    if (!player.getInventory().add(prevOffhand)) {
                        player.drop(prevOffhand, false);
                    }
                }
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(weapon));
            }
        }
    }

    private static boolean isCharacterWeapon(Item item) {
        for (CharacterType type : CharacterType.values()) {
            if (item == getWeapon(type)) {
                return true;
            }
        }
        // 은퇴한 예전 캐릭터 무기 — 인벤토리에 남아 있으면 캐릭터를 바꿀 때 같이 회수
        return item == ModItems.FIRE_BLADE.get();
    }

    // 캐릭터별 전용 장비 (장신구/방어구). 부위는 아이템 자체(ArmorItem.Type)가 안다.
    private static Item getArmor(CharacterType characterType) {
        return switch (characterType) {
            case MONKEY -> ModItems.MONKEY_CIRCLET.get();
            case GAMBLER -> ModItems.GAMBLER_FEDORA.get();
            case SWORDSMAN -> ModItems.SWORDSMAN_CREST.get();
            case HACKER -> ModItems.HACKER_GOGGLES.get();
            case HAMMER -> ModItems.HAMMER_MASK.get();
            case CYBORG -> ModItems.CYBORG_CHESTPLATE.get();
            case BERSERKER -> ModItems.BERSERKER_PELT.get();
            case DOJUK -> ModItems.DOJUK_HOODIE.get();
            case PHOENIX -> ModItems.PHOENIX_WINGS.get();
            default -> null;
        };
    }

    private static void giveCharacterArmor(ServerPlayer player, CharacterType characterType) {
        // 1) 이전 캐릭터의 전용 장비 전부 제거 (인벤토리 + 장비 슬롯 — containerSize가 갑옷칸 포함)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof com.example.examplemod.item.CharacterArmorItem) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        // 2) 새 캐릭터의 장비 착용 (원래 그 부위에 있던 일반 아이템은 인벤토리로)
        Item armor = getArmor(characterType);
        if (armor instanceof net.minecraft.world.item.ArmorItem armorItem) {
            net.minecraft.world.entity.EquipmentSlot slot = armorItem.getType().getSlot();
            ItemStack prev = player.getItemBySlot(slot);
            if (!prev.isEmpty() && !(prev.getItem() instanceof com.example.examplemod.item.CharacterArmorItem)) {
                if (!player.getInventory().add(prev)) {
                    player.drop(prev, false);
                }
            }
            player.setItemSlot(slot, new ItemStack(armor));
        }
    }

    // 전용 장비 강제 유지: 벗거나 옮겼으면 회수 후 재착용 (CharacterArmorEvents가 주기 호출)
    // + 복사본 청소: 착용 슬롯의 정품 1개 외의 전용 장비는 전부 삭제 (상자 등으로 복제해도 무용지물)
    public static void ensureCharacterArmor(ServerPlayer player) {
        CharacterType characterType = getCharacter(player);
        Item armor = getArmor(characterType);
        net.minecraft.world.entity.EquipmentSlot slot = null;
        if (armor instanceof net.minecraft.world.item.ArmorItem armorItem) {
            slot = armorItem.getType().getSlot();
            if (!player.getItemBySlot(slot).is(armor)) {
                giveCharacterArmor(player, characterType);
            }
        }

        // 인벤토리 전체에서 "착용 슬롯의 본인 캐릭터 장비" 외의 전용 장비를 제거
        // (갑옷 슬롯 인덱스 = 36 + slot.getIndex())
        int allowedIndex = slot == null ? -1 : 36 + slot.getIndex();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof com.example.examplemod.item.CharacterArmorItem)) {
                continue;
            }
            if (i == allowedIndex && armor != null && stack.is(armor)) {
                continue;   // 정품 착용본만 허용
            }
            player.getInventory().setItem(i, ItemStack.EMPTY);
        }

        // 마우스 커서에 잡고 있는 것까지 청소 — 재착용 타이밍에 낚아채는 복제 방지
        if (player.containerMenu.getCarried().getItem()
                instanceof com.example.examplemod.item.CharacterArmorItem) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
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

    // 접속 시 속성 방어력만 현재 수치로 재정렬 (체력·회복은 건드리지 않음 — 재접속 풀피 악용 방지)
    // 구버전에서 속성 +2로 저장된 불사조가 날개(+2)와 중복되는 것 정리용
    public static void reconcileBonusArmor(Player player) {
        AttributeInstance armorAttribute = player.getAttribute(Attributes.ARMOR);
        if (armorAttribute == null) {
            return;
        }
        armorAttribute.removeModifier(CHARACTER_ARMOR_MODIFIER_UUID);
        double bonusArmor = CharacterStats.getBonusArmor(getCharacter(player));
        if (bonusArmor != 0) {
            armorAttribute.addPermanentModifier(new AttributeModifier(
                    CHARACTER_ARMOR_MODIFIER_UUID,
                    "Character armor bonus",
                    bonusArmor,
                    AttributeModifier.Operation.ADDITION));
        }
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

    // 캐릭터 고유 방어력 (전용 방어구가 없는 캐릭터용 — 불사조 2 등)
    AttributeInstance armorAttribute = player.getAttribute(Attributes.ARMOR);
    if (armorAttribute != null) {
        armorAttribute.removeModifier(CHARACTER_ARMOR_MODIFIER_UUID);
        double bonusArmor = CharacterStats.getBonusArmor(characterType);
        if (bonusArmor != 0) {
            armorAttribute.addPermanentModifier(new AttributeModifier(
                    CHARACTER_ARMOR_MODIFIER_UUID,
                    "Character armor bonus",
                    bonusArmor,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    player.setHealth((float) player.getMaxHealth());
    }

    private static final java.util.UUID CHARACTER_ARMOR_MODIFIER_UUID =
            java.util.UUID.fromString("9f2e6a1b-3c4d-4e5f-8a7b-777777777771");
}