package com.example.examplemod.item;

import com.example.examplemod.client.ClientHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class CharacterSelectItem extends Item {

    public CharacterSelectItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openCharacterSelectScreen);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}