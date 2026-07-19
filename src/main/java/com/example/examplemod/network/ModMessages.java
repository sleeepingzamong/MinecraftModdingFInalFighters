package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExampleMod.MODID, "messages"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(
                packetId++,
                SelectCharacterC2SPacket.class,
                SelectCharacterC2SPacket::encode,
                SelectCharacterC2SPacket::new,
                SelectCharacterC2SPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                UseSkillC2SPacket.class,
                UseSkillC2SPacket::encode,
                UseSkillC2SPacket::new,
                UseSkillC2SPacket::handle
        );

        INSTANCE.registerMessage(
        packetId++,
        GuardC2SPacket.class,
        GuardC2SPacket::encode,
        GuardC2SPacket::new,
        GuardC2SPacket::handle
);

        INSTANCE.registerMessage(
                packetId++,
                CombatStatsS2CPacket.class,
                CombatStatsS2CPacket::encode,
                CombatStatsS2CPacket::new,
                CombatStatsS2CPacket::handle
        );

        INSTANCE.registerMessage(
        packetId++,
        DashC2SPacket.class,
        DashC2SPacket::encode,
        DashC2SPacket::new,
        DashC2SPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                LaserStateS2CPacket.class,
                LaserStateS2CPacket::encode,
                LaserStateS2CPacket::new,
                LaserStateS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                GunShootC2SPacket.class,
                GunShootC2SPacket::encode,
                GunShootC2SPacket::new,
                GunShootC2SPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                HammerSpinS2CPacket.class,
                HammerSpinS2CPacket::encode,
                HammerSpinS2CPacket::new,
                HammerSpinS2CPacket::handle
        );
    }

    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }

    // 특정 플레이어(클라이언트)에게 보내기
    public static void sendToPlayer(Object message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    // 이 플레이어를 보고 있는 모든 클라이언트 + 본인에게 보내기 (모두에게 보이는 연출용)
    public static void sendToTrackingAndSelf(Object message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
    }
}