package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라: 해커 X "버그 무시" 시작 — ticks 동안 바이러스 2마리가 그 플레이어 주변을 돈다
public class BugIgnoreS2CPacket {

    private final int entityId;
    private final int ticks;

    public BugIgnoreS2CPacket(int entityId, int ticks) {
        this.entityId = entityId;
        this.ticks = ticks;
    }

    public BugIgnoreS2CPacket(FriendlyByteBuf buf) {
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
                        com.example.examplemod.client.ClientBugIgnore.set(entityId, ticks)
                )
        );
        context.setPacketHandled(true);
    }
}
