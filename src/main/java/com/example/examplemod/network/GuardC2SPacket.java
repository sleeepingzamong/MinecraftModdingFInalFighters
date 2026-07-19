package com.example.examplemod.network;

import com.example.examplemod.combat.GuardManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GuardC2SPacket {

    private final boolean guarding;

    public GuardC2SPacket(boolean guarding) {
        this.guarding = guarding;
    }

    public GuardC2SPacket(FriendlyByteBuf buf) {
        this.guarding = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(guarding);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                GuardManager.setGuarding(player, guarding);
            }
        });

        context.setPacketHandled(true);
    }
}