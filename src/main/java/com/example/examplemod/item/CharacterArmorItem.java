package com.example.examplemod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// 캐릭터 전용 장비 (장신구/방어구): 캐릭터 선택 시 자동 착용, 벗기·버리기·드롭 불가
// 착용 텍스처는 assets/examplemod/textures/models/armor/<이름>_layer_1.png
public class CharacterArmorItem extends ArmorItem {

    private final String armorName;   // 착용 텍스처·커스텀 모델 이름

    public CharacterArmorItem(String textureName, int defense, Type type, Properties properties) {
        super(new CharacterArmorMaterial(textureName, defense, type), type, properties);
        this.armorName = textureName;
    }

    // 커스텀 3D 모델 연결 (얇은 머리테·모자 챙·후드 등 실제 형태 — 클라이언트에서만 호출됨)
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    net.minecraft.world.entity.LivingEntity livingEntity, ItemStack itemStack,
                    net.minecraft.world.entity.EquipmentSlot equipmentSlot,
                    net.minecraft.client.model.HumanoidModel<?> original) {
                return com.example.examplemod.client.armor.ClientArmorModels.get(armorName);
            }
        });
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;   // 내구도 없음
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;   // Q키로 버릴 수 없음
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("캐릭터 전용 장비").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }

    // 캐릭터별 방어력만 다른 단순 재질 (해당 부위에만 방어력 부여)
    private record CharacterArmorMaterial(String textureName, int defense, Type type) implements ArmorMaterial {

        @Override
        public int getDurabilityForType(Type t) {
            return 1000;   // isDamageable=false라 실사용 안 됨
        }

        @Override
        public int getDefenseForType(Type t) {
            return t == this.type ? this.defense : 0;
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "examplemod:" + textureName;   // 착용 텍스처 경로 결정
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    }
}
