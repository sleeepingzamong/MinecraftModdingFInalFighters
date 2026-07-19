package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.RevolverItem;
import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.registry.ModItems;
import com.example.examplemod.registry.ModParticles;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 커스텀 팔 자세를 미리 생성 (첫 렌더링 뒤에 만들면 크래시 — HammerPoses 주석 참고)
            HammerPoses.init();

            // 리볼버 "reload" 프로퍼티: 재장전 진행도 0~1 → 모델 overrides가 이 값으로 프레임 선택
            ItemProperties.register(ModItems.REVOLVER.get(),
                    new ResourceLocation(ExampleMod.MODID, "reload"),
                    (stack, level, entity, seed) -> RevolverItem.getReloadProgress(stack, entity));

            // 해머 "spinning" 프로퍼티: 회전 베기 중이면 1 → 끝을 잡은 모델(hammer_spin)로 교체
            ItemProperties.register(ModItems.HAMMER.get(),
                    new ResourceLocation(ExampleMod.MODID, "spinning"),
                    (stack, level, entity, seed) ->
                            ClientSpinState.isSpinning(entity) ? 1.0F : 0.0F);
        });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.SKILL_1_KEY);
        event.register(ModKeyMappings.SKILL_2_KEY);
        event.register(ModKeyMappings.ULTIMATE_KEY);
        event.register(ModKeyMappings.DASH_KEY);
        event.register(ModKeyMappings.GUARD_KEY);
        event.register(ModKeyMappings.CHARACTER_SELECT_KEY);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("combat_hud", CombatHudOverlay.HUD);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SEVEN.get(), GamblerParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SKULL.get(), GamblerParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SKILL_PROJECTILE.get(), SkillProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.CLONE.get(), CloneRenderer::new);
        // 훈련용 좀비: 바닐라 좀비 모델·텍스처 그대로 사용
        event.registerEntityRenderer(ModEntities.TRAINING_DUMMY.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.LASER_BEAM.get(), LaserBeamRenderer::new);
    }
}
