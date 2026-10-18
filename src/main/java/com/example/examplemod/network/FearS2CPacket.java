package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라: 공포에 걸린 플레이어 알림 (머리 위 귀신 표식 렌더링용)
public class FearS2CPacket {

    private final int entityId;
    private final int ticks;

    public FearS2CPacket(int entityId, int ticks) {
        this.entityId = entityId;
        this.ticks = ticks;
    }

    public FearS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.ticks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(ticks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientFearData.set(entityId, ticks)
                )
        );
        context.setPacketHandled(true);
    }
}
