package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.combat.SkillType;
import com.example.examplemod.network.DashC2SPacket;
import com.example.examplemod.network.GuardC2SPacket;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.UseSkillC2SPacket;
import com.example.examplemod.registry.ModItems;
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

    ModMessages.sendToServer(new DashC2SPacket(dirX, dirZ));
}   

        handleSkillCharge();

        boolean isGuarding = ModKeyMappings.GUARD_KEY.isDown();

        if (isGuarding != wasGuarding) {
            ModMessages.sendToServer(new GuardC2SPacket(isGuarding));
            wasGuarding = isGuarding;
        }
    }

    private static void handleSkillCharge() {
        // 톡 누른 클릭이 버퍼에 쌓여 isDown 상태가 꼬이는 것 방지 — 매 틱 비움
        while (ModKeyMappings.SKILL_1_KEY.consumeClick()) {}
        while (ModKeyMappings.SKILL_2_KEY.consumeClick()) {}
        while (ModKeyMappings.ULTIMATE_KEY.consumeClick()) {}

        boolean isSkill1Down = ModKeyMappings.SKILL_1_KEY.isDown();
        boolean isSkill2Down = ModKeyMappings.SKILL_2_KEY.isDown();
        boolean isUltimateDown = ModKeyMappings.ULTIMATE_KEY.isDown();

        boolean isChargingSkill = isSkill1Down || isSkill2Down || isUltimateDown;

        handleChargeFeedback(isChargingSkill);

        if (isSkill1Down) {
            skill1ChargeTick = Math.min(skill1ChargeTick + 1, MAX_CHARGE_TICK);
        }

        if (isSkill2Down) {
            skill2ChargeTick = Math.min(skill2ChargeTick + 1, MAX_CHARGE_TICK);
        }

        if (isUltimateDown) {
            ultimateChargeTick = Math.min(ultimateChargeTick + 1, MAX_CHARGE_TICK);
        }

        if (!isSkill1Down && wasSkill1Down) {
            ModMessages.sendToServer(new UseSkillC2SPacket(SkillType.SKILL_1, skill1ChargeTick));
            skill1ChargeTick = 0;
        }

        if (!isSkill2Down && wasSkill2Down) {
            ModMessages.sendToServer(new UseSkillC2SPacket(SkillType.SKILL_2, skill2ChargeTick));
            skill2ChargeTick = 0;
        }

        if (!isUltimateDown && wasUltimateDown) {
            ModMessages.sendToServer(new UseSkillC2SPacket(SkillType.ULTIMATE, ultimateChargeTick));
            ultimateChargeTick = 0;
        }

        wasSkill1Down = isSkill1Down;
        wasSkill2Down = isSkill2Down;
        wasUltimateDown = isUltimateDown;
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

    // HUD가 차징 진행도를 읽는 통로
    public static int getActiveChargeTick() {
        return Math.max(skill1ChargeTick, Math.max(skill2ChargeTick, ultimateChargeTick));
    }

    public static int getMaxChargeTick() {
        return MAX_CHARGE_TICK;
    }
}