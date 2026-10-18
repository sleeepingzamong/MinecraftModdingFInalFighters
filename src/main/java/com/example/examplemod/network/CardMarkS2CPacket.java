package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라(갬블러 본인): 블랙잭 카드 상태 (대상 머리 위 "N / 21" — 시전자에게만 보임)
// ticks <= 0 이면 표시 제거
public class CardMarkS2CPacket {

    private final int targetId;
    private final int sum;
    private final int ticks;

    public CardMarkS2CPacket(int targetId, int sum, int ticks) {
        this.targetId = targetId;
        this.sum = sum;
        this.ticks = ticks;
    }

    public CardMarkS2CPacket(FriendlyByteBuf buf) {
        this.targetId = buf.readInt();
        this.sum = buf.readInt();
        this.ticks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(targetId);
        buf.writeInt(sum);
        buf.writeInt(ticks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientCardMark.set(targetId, sum, ticks)
                )
        );
        context.setPacketHandled(true);
    }
}
