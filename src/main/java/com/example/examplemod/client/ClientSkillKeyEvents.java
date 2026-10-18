package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.combat.SkillType;
import com.example.examplemod.network.DashC2SPacket;
import com.example.examplemod.network.GuardC2SPacket;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.UseSkillC2SPacket;
import com.example.examplemod.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class ClientSkillKeyEvents {

    private static boolean wasGuarding = false;

    private static boolean wasSkill1Down = false;
    private static boolean wasSkill2Down = false;
    private static boolean wasUltimateDown = false;

    private static int swingTimer = 0;

    private static int skill1ChargeTick = 0;
    private static int skill2ChargeTick = 0;
    private static int ultimateChargeTick = 0;

    private static final int MAX_CHARGE_TICK = 40;

    // 달리기 키와 대쉬 키가 같은 키면(둘 다 기본값 왼쪽 Ctrl) Forge는 한 키의 눌림을 한쪽(대쉬)에만 줘서 달리기가 안 됨 →
    // 틱 시작마다 실제 키 눌림을 직접 읽어 달리기 키에도 전달 (대쉬는 그대로 나가고, 누르고 있으면 계속 달림)
    private static boolean sprintMirrored = false;

    @SubscribeEvent
    public static void onClientTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        KeyMapping sprint = mc.options.keySprint;
        InputConstants.Key key = sprint.getKey();
        boolean held = mc.player != null && mc.screen == null
                && !sprint.isUnbound() && key.equals(ModKeyMappings.DASH_KEY.getKey())
                && key.getType() == InputConstants.Type.KEYSYM
                && InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
        if (held != sprintMirrored) {   // 바뀔 때만 전달 — "달리기 전환" 설정이어도 한 번 누름 = 한 번 전환
            sprint.setDown(held);
            sprintMirrored = held;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (Minecraft.getInstance().screen != null) {
            return;
        }

        LocalPlayer dashPlayer = Minecraft.getInstance().player;
while (ModKeyMappings.DASH_KEY.consumeClick()) {
    if (dashPlayer == null) {
        continue;
    }
    float forward = dashPlayer.input.forwardImpulse;   // W:+ S:-
    float strafe = dashPlayer.input.leftImpulse;       // A:+ D:-

    double yawRad = Math.toRadians(dashPlayer.getYRot());
    double sin = Math.sin(yawRad);
    double cos = Math.cos(yawRad);

    // WASD(상대) → 월드 방향으로 회전
    double dirX = strafe * cos - forward * sin;
    double dirZ = forward * cos + strafe * sin;

    if (ClientCombatData.getStamina() < 1) {
        HudFx.triggerLowStamina();   // 스태미나 없음 → 바 흔들림 피드백
    }
    ModMessages.sendToServer(new DashC2SPacket(dirX, dirZ));
}

        handleSkillCharge();

        // 스턴 중엔 가드 불가 — 키를 누르고 있어도 꺼진 것으로 보내고, 스턴이 풀렸을 때 계속 누르고 있으면 다시 켜짐
        boolean isGuarding = ModKeyMappings.GUARD_KEY.isDown() && !isStunnedNow();

        if (isGuarding != wasGuarding) {
            ModMessages.sendToServer(new GuardC2SPacket(isGuarding));
            wasGuarding = isGuarding;
        }
    }

    private static void handleSkillCharge() {
        boolean monkey = "MONKEY".equals(ClientCombatData.getCharacter());

        // ===== 궁: 전 캐릭터 누르는 즉시 발동 (차징 제거) =====
        while (ModKeyMappings.ULTIMATE_KEY.consumeClick()) {
            if (ClientCombatData.getUltimateGauge() < 100) {
                HudFx.triggerUltNotReady();   // 게이지 부족 → 궁 바 흔들림 피드백
            }
            ModMessages.sendToServer(new UseSkillC2SPacket(SkillType.ULTIMATE, 0));
        }

        // ===== 손오공 외: Z/X도 누르는 즉시 발동 (차징이 의미 없는 캐릭터는 반응성 우선) =====
        if (!monkey) {
            while (ModKeyMappings.SKILL_1_KEY.consumeClick()) {
                if (ClientCombatData.getStamina() < 1) {
                    HudFx.triggerLowStamina();
                }
                ModMessages.sendToServer(new UseSkillC2SPacket(SkillType.SKILL_1, 0));
            }
            while (ModKeyMappings.SKILL_2_KEY.consumeClick()) {
                if (ClientCombatData.getStamina() < 1) {
                    HudFx.triggerLowStamina();
                }
                ModMessages.sendToServer(new UseSkillC2SPacket(SkillType.SKILL_2, 0));
            }
            skill1ChargeTick = 0;
            skill2ChargeTick = 0;
            ultimateChargeTick = 0;
            wasSkill1Down = false;
            wasSkill2Down = false;
            wasUltimateDown = false;
            swingTimer = 0;
            return;
        }

        // ===== 손오공: Z/X 홀드 차징 유지 (놓는 순간 발동) =====
        // 톡 누른 클릭이 버퍼에 쌓여 isDown 상태가 꼬이는 것 방지 — 매 틱 비움
        while (ModKeyMappings.SKILL_1_KEY.consumeClick()) {}
        while (ModKeyMappings.SKILL_2_KEY.consumeClick()) {}

        boolean isSkill1Down = ModKeyMappings.SKILL_1_KEY.isDown();
        boolean isSkill2Down = ModKeyMappings.SKILL_2_KEY.isDown();

        handleChargeFeedback(isSkill1Down || isSkill2Down);

        if (isSkill1Down) {
            skill1ChargeTick = Math.min(skill1ChargeTick + 1, MAX_CHARGE_TICK);
        }

        if (isSkill2Down) {
            skill2ChargeTick = Math.min(skill2ChargeTick + 1, MAX_CHARGE_TICK);
        }

        if (!isSkill1Down && wasSkill1Down) {
            if (ClientCombatData.getStamina() < 1) {
                HudFx.triggerLowStamina();
            }
            ModMessages.sendToServer(new UseSkillC2SPacket(SkillType.SKILL_1, skill1ChargeTick));
            skill1ChargeTick = 0;
        }

        if (!isSkill2Down && wasSkill2Down) {
            if (ClientCombatData.getStamina() < 1) {
                HudFx.triggerLowStamina();
            }
            ModMessages.sendToServer(new UseSkillC2SPacket(SkillType.SKILL_2, skill2ChargeTick));
            skill2ChargeTick = 0;
        }

        wasSkill1Down = isSkill1Down;
        wasSkill2Down = isSkill2Down;
    }

    private static void handleChargeFeedback(boolean isChargingSkill) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (!isChargingSkill) {
            swingTimer = 0;
            return;
        }

        boolean holdingBang = player.getMainHandItem().is(ModItems.RUYI_JINGU_BANG.get());
        if (!holdingBang) {
            return;
        }

        // 봉 휘두르기 반복 (약 0.4초마다 한 번)
        swingTimer++;
        if (swingTimer >= 8) {
            player.swing(InteractionHand.MAIN_HAND);
            swingTimer = 0;
        }

        // 기 모으는 파티클 (차징할수록 많아짐)
        spawnChargeParticles(player, getActiveChargeTick());
    }

    private static void spawnChargeParticles(LocalPlayer player, int chargeTick) {
        int count = 1 + chargeTick / 8;
        double radius = 1.0;
        double baseAngle = player.tickCount * 0.25;

        for (int k = 0; k < count; k++) {
            double angle = baseAngle + k * (Math.PI * 2 / count);
            double px = player.getX() + Math.cos(angle) * radius;
            double py = player.getY() + 0.5 + Math.random();
            double pz = player.getZ() + Math.sin(angle) * radius;

            player.level().addParticle(
                    ParticleTypes.CRIT,
                    px, py, pz,
                    -Math.cos(angle) * 0.05, 0.05, -Math.sin(angle) * 0.05
            );
        }
    }

    // 가드 키를 누르고 있는지 (가드 비네트·방패 아이콘 표시용)
    public static boolean isGuardingNow() {
        return wasGuarding;
    }

    // 내가 스턴 중인지 — 서버가 스턴과 함께 거는 상태 효과(STUNNED)로 판단 (평타·가드 입력 차단용)
    public static boolean isStunnedNow() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(com.example.examplemod.registry.ModEffects.STUNNED.get());
    }

    // 궁극기 키를 차징 중인지 (캐릭터별 궁 대기모션 판별용)
    public static int getUltimateChargeTick() {
        return ultimateChargeTick;
    }

    // HUD가 차징 진행도를 읽는 통로
    public static int getActiveChargeTick() {
        return Math.max(skill1ChargeTick, Math.max(skill2ChargeTick, ultimateChargeTick));
    }

    public static int getMaxChargeTick() {
        return MAX_CHARGE_TICK;
    }
}