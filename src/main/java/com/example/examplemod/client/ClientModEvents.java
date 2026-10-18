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
    }

    private static final ResourceLocation LOW_HEALTH_VIGNETTE =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/low_health_vignette.png");
    private static final ResourceLocation GUARD_VIGNETTE =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/guard_vignette.png");

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("combat_hud", CombatHudOverlay.HUD);

        // 빈사 비네트: HUD 아래에 깔리는 붉은 화면 테두리 (심장박동에 맞춰 펄스)
        event.registerBelowAll("low_health_vignette", (gui, guiGraphics, partialTick, width, height) -> {
            float alpha = LowHealthEffects.vignetteAlpha(partialTick);
            if (alpha <= 0f) {
                return;
            }
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            guiGraphics.setColor(1f, 1f, 1f, alpha);
            guiGraphics.blit(LOW_HEALTH_VIGNETTE, 0, 0, width, height, 0f, 0f, 256, 256, 256, 256);
            guiGraphics.setColor(1f, 1f, 1f, 1f);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        });

        // 가드 비네트: 가드 중 푸른 화면 테두리, 피해를 막아내면 진해졌다 풀림
        event.registerBelowAll("guard_vignette", (gui, guiGraphics, partialTick, width, height) -> {
            float alpha = ClientGuardFx.vignetteAlpha(partialTick);
            if (alpha <= 0f) {
                return;
            }
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            guiGraphics.setColor(1f, 1f, 1f, alpha);
            guiGraphics.blit(GUARD_VIGNETTE, 0, 0, width, height, 0f, 0f, 256, 256, 256, 256);
            guiGraphics.setColor(1f, 1f, 1f, 1f);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        });
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SEVEN.get(), GamblerParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SKULL.get(), GamblerParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RAGE.get(), RageParticle.Provider::new);
        // 바닐라 블록 부스러기 공급자를 필터 버전으로 교체 — 도적 발밑 달리기·착지 잔해 제거
        event.registerSpecial(net.minecraft.core.particles.ParticleTypes.BLOCK,
                new NoTraceBlockParticleProvider());
    }

    // 캐릭터 장비 커스텀 모델 지오메트리 등록
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (String name : com.example.examplemod.client.armor.ClientArmorModels.NAMES) {
            event.registerLayerDefinition(
                    com.example.examplemod.client.armor.ClientArmorModels.layerLocation(name),
                    () -> com.example.examplemod.client.armor.CharacterArmorModel.createLayer(name));
        }
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
