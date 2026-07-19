package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.RevolverItem;
import com.example.examplemod.network.GunShootC2SPacket;
import com.example.examplemod.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 리볼버를 들고 있으면 좌클릭(공격)을 가로채서 발사로 바꾼다
@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class GunInputEvents {

    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;   // 좌클릭만
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof RevolverItem revolver)) {
            return;   // 총 안 들었으면 평소대로 (근접·블록 캐기 정상)
        }

        // 총을 든 좌클릭: 근접 공격/블록 캐기 차단
        event.setCanceled(true);

        if (player.getCooldowns().isOnCooldown(revolver)) {
            event.setSwingHand(false);   // 재장전/연사 대기 중엔 팔도 안 휘두름
            return;
        }

        event.setSwingHand(true);        // 발사 모션
        ModMessages.sendToServer(new GunShootC2SPacket());
    }
}
