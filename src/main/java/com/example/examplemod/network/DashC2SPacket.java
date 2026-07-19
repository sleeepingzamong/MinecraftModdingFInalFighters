package com.example.examplemod.network;

import com.example.examplemod.combat.SkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DashC2SPacket {

    private final double dirX;
    private final double dirZ;

    public DashC2SPacket(double dirX, double dirZ) {
        this.dirX = dirX;
        this.dirZ = dirZ;
    }

    public DashC2SPacket(FriendlyByteBuf buf) {
        this.dirX = buf.readDouble();
        this.dirZ = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(dirX);
        buf.writeDouble(dirZ);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SkillManager.useDash(player, dirX, dirZ);
            }
        });
        context.setPacketHandled(true);
    }
}