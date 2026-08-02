package com.example.examplemod.network;

import com.example.examplemod.combat.CyborgPunchActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 클라 → 서버: 사이보그 너클 펀치 (leftHand=true 왼손 / false 오른손)
public class PunchC2SPacket {

    private final boolean leftHand;

    public PunchC2SPacket(boolean leftHand) {
        this.leftHand = leftHand;
    }

    public PunchC2SPacket(FriendlyByteBuf buf) {
        this.leftHand = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(leftHand);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CyborgPunchActions.punch(player, leftHand);
            }
        });
        context.setPacketHandled(true);
    }
}
