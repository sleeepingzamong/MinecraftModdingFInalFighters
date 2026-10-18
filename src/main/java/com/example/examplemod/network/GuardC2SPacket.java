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

            if (player != null && !player.isSpectator()) {   // 관전 중 방어 불가
                // 스턴 중엔 가드를 켤 수 없음 (끄는 건 언제나 허용)
                if (guarding && com.example.examplemod.combat.StunManager.isStunned(player)) {
                    return;
                }
                GuardManager.setGuarding(player, guarding);
            }
        });

        context.setPacketHandled(true);
    }
}