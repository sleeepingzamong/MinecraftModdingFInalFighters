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
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class PlayerAnimations {

    // PlayAnimationS2CPacket이 호출 — 대상 플레이어를 찾아 시전 모션 재생
    public static void playCastAnimation(int entityId, String animName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (mc.level.getEntity(entityId) instanceof AbstractClientPlayer clientPlayer) {
            Tick.playCastAnimationOnLayer(clientPlayer,
                    new ResourceLocation(ExampleMod.MODID, animName));
        }
    }

    // 플레이어에 달아둘 "애니메이션 슬롯"의 고유 키
    private static final ResourceLocation ANIM_LAYER = new ResourceLocation(ExampleMod.MODID, "charging_layer");
    // charging.json 애니메이션을 부르는 키 (modid:이름)
    private static final ResourceLocation CHARGING_ANIM = new ResourceLocation(ExampleMod.MODID, "charging");

    // 해머 회전 베기 자세 (hammer_spin.json) — 회전 중인 "모든" 플레이어에게 적용
    private static final ResourceLocation SPIN_LAYER = new ResourceLocation(ExampleMod.MODID, "hammer_spin_layer");
    private static final ResourceLocation SPIN_ANIM = new ResourceLocation(ExampleMod.MODID, "hammer_spin");

    // 해머 궁극기: 차징 대기모션 + 시전(1회성) 모션
    private static final ResourceLocation HAMMER_ULT_WAIT_ANIM = new ResourceLocation(ExampleMod.MODID, "hammer_ultimate_waiting");
    // 궁 대기모션 전용 슬롯 (전용 무릎 굽힘이 붙어 있어 기본 차징과 분리)
    private static final ResourceLocation WAIT_LAYER = new ResourceLocation(ExampleMod.MODID, "hammer_wait_layer");
    // 1회성 시전 모션 전용 슬롯 (서버가 PlayAnimationS2CPacket으로 재생 지시)
    private static final ResourceLocation CAST_LAYER = new ResourceLocation(ExampleMod.MODID, "cast_layer");

    // 기본 이동 모션 (모든 캐릭터 공통): 걷기/달리기/뒷걸음 — 스킬 모션들보다 아래 깔림
    // Animation Overhaul 도입으로 기본 꺼둠 (둘 다 켜면 같은 부위를 두고 싸움) — AO를 빼면 true로
    private static final boolean USE_CUSTOM_LOCOMOTION = false;
    private static final ResourceLocation LOCO_LAYER = new ResourceLocation(ExampleMod.MODID, "locomotion_layer");

    // 양손 무기(BC 대기 자세 보유) 달리기 보정: BC 자세가 AO 달리기와 겹치면 팔이 위로 붕 뜨므로,
    // 달리는 동안만 무기별 "달리기 팔 자세"를 BC보다 높은 우선순위로 덮어씌운다
    private static final ResourceLocation RUN_LOWER_LAYER = new ResourceLocation(ExampleMod.MODID, "run_lower_layer");
    private static final ResourceLocation RUN_LOWER_ANIM = new ResourceLocation(ExampleMod.MODID, "run_lower");     // 해머·도끼 (내려 잡기)
    private static final ResourceLocation RUN_SCYTHE_ANIM = new ResourceLocation(ExampleMod.MODID, "run_scythe");   // 낫 (대기 자세 그대로)
    private static final ResourceLocation RUN_KATANA_ANIM = new ResourceLocation(ExampleMod.MODID, "run_katana");   // 카타나 (대기 자세 + 아래·앞)

    // 궁 시전 중 무릎 굽힘 키프레임 {틱, 각도} — 블록벤치의 종아리(_down) 키프레임에서 옮김
    private static final float[][] HAMMER_ULT_BEND_RIGHT = {
            {0F, 15F}, {10F, -2.5F}, {15F, 112.2F}, {21.67F, -4.89F}, {26.67F, -4.89F}
    };
    private static final float[][] HAMMER_ULT_BEND_LEFT = {
            {0F, 52.5F}, {5.83F, -7.5F}, {10F, -7.5F}, {15F, 106.6F}, {21.67F, 12.77F}, {26.67F, 12.77F}
    };

    // ===== 이동 모션 관절 트랙 {틱, 각도} — 순서: 오른무릎, 왼무릎, 오른팔꿈치, 왼팔꿈치 =====
    // 무릎: 다리가 뒤에서 앞으로 스윙할 때(지나가는 순간) 가장 많이 접힘
    private static final float[][][] WALK_BENDS = {
            {{0F, 5F}, {5F, 10F}, {10F, 20F}, {15F, 45F}, {20F, 5F}},
            {{0F, 20F}, {5F, 45F}, {10F, 5F}, {15F, 10F}, {20F, 20F}},
            {{0F, 10F}, {10F, 25F}, {20F, 10F}},
            {{0F, 25F}, {10F, 10F}, {20F, 25F}}
    };
    private static final float[][][] RUN_BENDS = {
            {{0F, 10F}, {3.5F, 15F}, {7F, 30F}, {10.5F, 65F}, {14F, 10F}},
            {{0F, 30F}, {3.5F, 65F}, {7F, 10F}, {10.5F, 15F}, {14F, 30F}},
            {{0F, 40F}, {7F, 70F}, {14F, 40F}},
            {{0F, 70F}, {7F, 40F}, {14F, 70F}}
    };
    private static final float[][][] WALK_BACK_BENDS = {
            {{0F, 5F}, {5F, 8F}, {10F, 12F}, {15F, 30F}, {20F, 5F}},
            {{0F, 12F}, {5F, 30F}, {10F, 5F}, {15F, 8F}, {20F, 12F}},
            {{0F, 12F}},
            {{0F, 12F}}
    };

    // 플레이어별 이동 관절 모디파이어 (팩토리에서 등록, 틱에서 트랙 교체)
    private static final java.util.Map<net.minecraft.client.player.AbstractClientPlayer, LimbBendModifier> LOCO_MODIFIERS =
            new java.util.WeakHashMap<>();

    private static boolean playing = false;

    // === 3단계) 셋업: 모든 플레이어에 빈 슬롯 등록 (MOD 버스) ===
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Setup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        ANIM_LAYER, 42, player -> new ModifierLayer<>());
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        SPIN_LAYER, 44, player -> {
                            // 회전 자세: 무릎 45도 고정 굽힘
                            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
                            layer.addModifierBefore(LimbBendModifier.legs(45F, 45F));
                            return layer;
                        });
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        WAIT_LAYER, 43, player -> {
                            // 궁 대기 자세: 오른 무릎 15도, 왼 무릎 52.5도 (블록벤치 종아리 값)
                            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
                            layer.addModifierBefore(LimbBendModifier.legs(15F, 52.5F));
                            return layer;
                        });
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        CAST_LAYER, 46, player -> {
                            // 궁 시전: 무릎 굽힘이 키프레임 따라 움직임 (점프 때 접힘 → 착지 때 펴짐)
                            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
                            layer.addModifierBefore(LimbBendModifier.legs(HAMMER_ULT_BEND_RIGHT, HAMMER_ULT_BEND_LEFT));
                            return layer;
                        });
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        RUN_LOWER_LAYER, 5000, player -> new ModifierLayer<>());   // BC 자세(~2000)보다 위
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        LOCO_LAYER, 38, player -> {
                            // 이동 모션 (맨 밑에 깔림) + 관절 굽힘 — 트랙은 걷기/뛰기 전환 시 틱에서 교체
                            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
                            LimbBendModifier bends = new LimbBendModifier();
                            layer.addModifierBefore(bends);
                            LOCO_MODIFIERS.put(player, bends);
                            return layer;
                        });
            });
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

            ModifierLayer<IAnimation> chargeLayer = (ModifierLayer<IAnimation>)
                    PlayerAnimationAccess.getPlayerAssociatedData(player).get(ANIM_LAYER);
            ModifierLayer<IAnimation> waitLayer = (ModifierLayer<IAnimation>)
                    PlayerAnimationAccess.getPlayerAssociatedData(player).get(WAIT_LAYER);

            if (charging) {
                // 해머 캐릭터가 궁극기 키(C)를 차징 중이면 전용 대기모션(무릎 굽힘 포함), 아니면 기본 차징 자세
                boolean hammerUltCharge = "HAMMER".equals(ClientCombatData.getCharacter())
                        && ClientSkillKeyEvents.getUltimateChargeTick() > 0;
                ModifierLayer<IAnimation> target = hammerUltCharge ? waitLayer : chargeLayer;
                KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(
                        hammerUltCharge ? HAMMER_ULT_WAIT_ANIM : CHARGING_ANIM);
                if (target != null && anim != null) {
                    target.setAnimation(new KeyframeAnimationPlayer(anim));   // 자세 켜기
                }
            } else {
                // 자세 끄기 (어느 쪽이 켜져 있었든 둘 다 정리)
                if (chargeLayer != null) {
                    chargeLayer.setAnimation(null);
                }
                if (waitLayer != null) {
                    waitLayer.setAnimation(null);
                }
            }
        }

        // 플레이어별 현재 적용 중인 달리기 팔 자세 (없으면 미적용)
        private static final java.util.Map<Integer, ResourceLocation> RUN_POSE_ACTIVE = new java.util.HashMap<>();

        // 무기별 달리기 팔 자세 (BC 대기 자세가 있는 무기만) — null이면 보정 없음
        private static ResourceLocation runPoseFor(net.minecraft.world.item.ItemStack stack) {
            if (stack.getItem() instanceof com.example.examplemod.item.HammerItem
                    || stack.getItem() instanceof com.example.examplemod.item.BattleAxeItem) {
                return RUN_LOWER_ANIM;
            }
            if (stack.getItem() instanceof com.example.examplemod.item.CyberScytheItem) {
                return RUN_SCYTHE_ANIM;
            }
            if (stack.getItem() instanceof com.example.examplemod.item.KatanaItem) {
                return RUN_KATANA_ANIM;
            }
            return null;
        }

        // 달리기 자세 전환용 짧은 페이드 (전환이 튀지 않게)
        private static dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier runPoseFade() {
            return dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier.standardFadeIn(
                    5, dev.kosmx.playerAnim.core.util.Ease.INOUTSINE);
        }

        // 서버 지시로 1회성 시전 모션 재생 (본인 + 상대 화면 모두)
        @SuppressWarnings("unchecked")
        public static void playCastAnimationOnLayer(AbstractClientPlayer clientPlayer, ResourceLocation animKey) {
            ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>)
                    PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(CAST_LAYER);
            if (layer == null) {
                return;
            }
            KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animKey);
            if (anim != null) {
                KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(anim);   // 비루프라 끝나면 자동 종료
                // 1인칭에서도 모션이 보이게 — 본인 화면에 팔·아이템을 3인칭 모델로 렌더
                animPlayer.setFirstPersonMode(dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.THIRD_PERSON_MODEL);
                animPlayer.setFirstPersonConfiguration(
                        new dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration()
                                .setShowRightArm(true)
                                .setShowLeftArm(true)
                                .setShowRightItem(true)
                                .setShowLeftItem(true));
                layer.setAnimation(animPlayer);
            }
        }

        // 플레이어별 현재 이동 모션 상태 ("walk"/"run"/"walk_back"/null)
        private static final java.util.Map<Integer, String> LOCO_STATE = new java.util.HashMap<>();

        // 해머 회전 + 기본 이동 모션: 월드의 모든 플레이어(본인+상대)에게 적용
        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public static void onSpinTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (Minecraft.getInstance().level == null) {
                LOCO_STATE.clear();
                RUN_POSE_ACTIVE.clear();
                return;
            }

            for (AbstractClientPlayer clientPlayer : Minecraft.getInstance().level.players()) {
                // ===== 해머 회전 베기 자세 =====
                ModifierLayer<IAnimation> spinLayer = (ModifierLayer<IAnimation>)
                        PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(SPIN_LAYER);
                if (spinLayer != null) {
                    boolean spinning = ClientSpinState.isSpinning(clientPlayer);
                    boolean active = spinLayer.getAnimation() != null;
                    if (spinning && !active) {
                        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(SPIN_ANIM);
                        if (anim != null) {
                            spinLayer.setAnimation(new KeyframeAnimationPlayer(anim));
                        }
                    } else if (!spinning && active) {
                        spinLayer.setAnimation(null);
                    }
                }

                // ===== 양손 무기 달리기: 무기별 달리기 팔 자세로 BC 대기 자세 덮어쓰기 =====
                ModifierLayer<IAnimation> runLowerLayer = (ModifierLayer<IAnimation>)
                        PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(RUN_LOWER_LAYER);
                if (runLowerLayer != null) {
                    // 회전 베기 중엔 달리기 팔 자세를 적용하지 않음 (회전 자세를 덮어써서 모델이 망가짐)
                    ResourceLocation wantedPose = clientPlayer.isSprinting()
                            && !ClientSpinState.isSpinning(clientPlayer)
                            ? runPoseFor(clientPlayer.getMainHandItem()) : null;
                    ResourceLocation activePose = RUN_POSE_ACTIVE.get(clientPlayer.getId());
                    if (!java.util.Objects.equals(wantedPose, activePose)) {
                        if (wantedPose == null) {
                            RUN_POSE_ACTIVE.remove(clientPlayer.getId());
                            runLowerLayer.replaceAnimationWithFade(runPoseFade(), null);
                        } else {
                            KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(wantedPose);
                            if (anim != null) {
                                RUN_POSE_ACTIVE.put(clientPlayer.getId(), wantedPose);
                                runLowerLayer.replaceAnimationWithFade(runPoseFade(),
                                        new KeyframeAnimationPlayer(anim));
                            }
                        }
                    }
                }

                // ===== 기본 이동 모션 (걷기/달리기/뒷걸음) =====
                if (!USE_CUSTOM_LOCOMOTION) {
                    continue;   // Animation Overhaul이 이동 모션 담당
                }
                ModifierLayer<IAnimation> locoLayer = (ModifierLayer<IAnimation>)
                        PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(LOCO_LAYER);
                if (locoLayer == null) {
                    continue;
                }

                String wanted = null;
                if (clientPlayer.onGround() && clientPlayer.walkAnimation.speed() > 0.05F) {
                    double dx = clientPlayer.getX() - clientPlayer.xo;
                    double dz = clientPlayer.getZ() - clientPlayer.zo;
                    if (dx * dx + dz * dz > 1.0E-5) {
                        // 이동 방향과 시선 방향의 내적 < 0 → 뒷걸음
                        double yawRad = Math.toRadians(clientPlayer.getYRot());
                        double lookX = -Math.sin(yawRad);
                        double lookZ = Math.cos(yawRad);
                        boolean backward = dx * lookX + dz * lookZ < -0.02;
                        wanted = backward ? "walk_back"
                                : (clientPlayer.isSprinting() ? "run" : "walk");
                    }
                }

                String current = LOCO_STATE.get(clientPlayer.getId());
                if (!java.util.Objects.equals(wanted, current)) {
                    LOCO_STATE.put(clientPlayer.getId(), wanted);
                    if (wanted == null) {
                        locoLayer.setAnimation(null);
                    } else {
                        // 모션에 맞는 관절(무릎·팔꿈치) 트랙으로 교체
                        // 팩토리에서 등록 못 된 경우를 대비해 없으면 즉석에서 부착
                        LimbBendModifier bends = LOCO_MODIFIERS.get(clientPlayer);
                        if (bends == null) {
                            bends = new LimbBendModifier();
                            locoLayer.addModifierBefore(bends);
                            LOCO_MODIFIERS.put(clientPlayer, bends);
                        }
                        float[][][] tracks = switch (wanted) {
                            case "run" -> RUN_BENDS;
                            case "walk_back" -> WALK_BACK_BENDS;
                            default -> WALK_BENDS;
                        };
                        bends.setTracks(tracks[0], tracks[1], tracks[2], tracks[3]);
                        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(
                                new ResourceLocation(ExampleMod.MODID, wanted));
                        locoLayer.setAnimation(anim != null ? new KeyframeAnimationPlayer(anim) : null);
                    }
                }
            }
        }
    }
}