package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라(도적 본인에게만): 표식 대상 발광 켜기/끄기 (ticks<=0 = 끄기)
// 발광은 받은 클라이언트에서만 로컬로 적용 — 다른 플레이어에게는 안 보임
public class MarkGlowS2CPacket {

    private final int entityId;
    private final int ticks;

    public MarkGlowS2CPacket(int entityId, int ticks) {
        this.entityId = entityId;
        this.ticks = ticks;
    }

    public MarkGlowS2CPacket(FriendlyByteBuf buf) {
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
                        com.example.examplemod.client.ClientMarkGlow.set(entityId, ticks)
                )
        );
        context.setPacketHandled(true);
    }
}
