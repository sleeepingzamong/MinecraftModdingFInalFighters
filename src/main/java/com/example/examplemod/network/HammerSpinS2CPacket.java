package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 해머 회전 베기 시작/종료 알림 (그 플레이어를 보는 모든 클라 + 본인)
// 클라는 이걸 받고 해당 플레이어의 모델을 돌리고, 본인이면 3인칭으로 전환한다.
public class HammerSpinS2CPacket {

    private final int playerId;
    private final boolean spinning;

    public HammerSpinS2CPacket(int playerId, boolean spinning) {
        this.playerId = playerId;
        this.spinning = spinning;
    }

    public HammerSpinS2CPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.spinning = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeBoolean(spinning);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientSpinState.setSpinning(playerId, spinning)
                )
        );
        context.setPacketHandled(true);
    }
}
