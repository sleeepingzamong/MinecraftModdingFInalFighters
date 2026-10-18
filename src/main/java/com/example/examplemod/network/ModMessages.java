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

        INSTANCE.registerMessage(
                packetId++,
                OpenSelectScreenS2CPacket.class,
                OpenSelectScreenS2CPacket::encode,
                OpenSelectScreenS2CPacket::new,
                OpenSelectScreenS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                PlayAnimationS2CPacket.class,
                PlayAnimationS2CPacket::encode,
                PlayAnimationS2CPacket::new,
                PlayAnimationS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                ParryMarksS2CPacket.class,
                ParryMarksS2CPacket::encode,
                ParryMarksS2CPacket::new,
                ParryMarksS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                SystemHackS2CPacket.class,
                SystemHackS2CPacket::encode,
                SystemHackS2CPacket::new,
                SystemHackS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                BugIgnoreS2CPacket.class,
                BugIgnoreS2CPacket::encode,
                BugIgnoreS2CPacket::new,
                BugIgnoreS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                CooldownS2CPacket.class,
                CooldownS2CPacket::encode,
                CooldownS2CPacket::new,
                CooldownS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                FearS2CPacket.class,
                FearS2CPacket::encode,
                FearS2CPacket::new,
                FearS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                RageS2CPacket.class,
                RageS2CPacket::encode,
                RageS2CPacket::new,
                RageS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                MarkGlowS2CPacket.class,
                MarkGlowS2CPacket::encode,
                MarkGlowS2CPacket::new,
                MarkGlowS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                GuardBlockS2CPacket.class,
                GuardBlockS2CPacket::encode,
                GuardBlockS2CPacket::new,
                GuardBlockS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                HitMarkerS2CPacket.class,
                HitMarkerS2CPacket::encode,
                HitMarkerS2CPacket::new,
                HitMarkerS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                CardMarkS2CPacket.class,
                CardMarkS2CPacket::encode,
                CardMarkS2CPacket::new,
                CardMarkS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                DamagePopupS2CPacket.class,
                DamagePopupS2CPacket::encode,
                DamagePopupS2CPacket::new,
                DamagePopupS2CPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                PhoenixZoneS2CPacket.class,
                PhoenixZoneS2CPacket::encode,
                PhoenixZoneS2CPacket::new,
                PhoenixZoneS2CPacket::handle
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

    // 이 엔티티(몹 포함)를 보고 있는 모든 클라이언트에게 보내기 (엔티티가 플레이어면 본인 포함)
    public static void sendToTracking(Object message, net.minecraft.world.entity.Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    // 같은 차원의 모든 클라이언트에게 보내기 (바닥 장판처럼 특정 엔티티에 안 붙은 연출용)
    public static void sendToDimension(Object message,
                                       net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        INSTANCE.send(PacketDistributor.DIMENSION.with(() -> dimension), message);
    }
}