package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class PlayerAnimations {

    // 플레이어에 달아둘 "애니메이션 슬롯"의 고유 키
    private static final ResourceLocation ANIM_LAYER = new ResourceLocation(ExampleMod.MODID, "charging_layer");
    // charging.json 애니메이션을 부르는 키 (modid:이름)
    private static final ResourceLocation CHARGING_ANIM = new ResourceLocation(ExampleMod.MODID, "charging");

    private static boolean playing = false;

    // === 3단계) 셋업: 모든 플레이어에 빈 슬롯 등록 (MOD 버스) ===
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Setup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() ->
                    PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                            ANIM_LAYER, 42, player -> new ModifierLayer<>()));
        }
    }

    // === 4단계) 매 틱: 차징 시작/끝에 자세 켜고 끄기 (FORGE 버스) ===
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class Tick {
        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            boolean charging = ClientSkillKeyEvents.getActiveChargeTick() > 0;
            if (charging == playing) {
                return;   // 상태 변화 없으면 패스 (매 틱 재시작 방지)
            }
            playing = charging;

            ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>)
                    PlayerAnimationAccess.getPlayerAssociatedData(player).get(ANIM_LAYER);
            if (layer == null) {
                return;
            }

            if (charging) {
                KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(CHARGING_ANIM);
                if (anim != null) {
                    layer.setAnimation(new KeyframeAnimationPlayer(anim));   // 자세 켜기
                }
            } else {
                layer.setAnimation(null);   // 자세 끄기 (null 허용)
            }
        }
    }
}