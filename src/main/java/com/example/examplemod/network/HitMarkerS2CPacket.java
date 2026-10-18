package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라(공격자): 적중 확인 → 크로스헤어 히트마커 (kill=true면 처치 마커: 빨갛고 크게)
public class HitMarkerS2CPacket {

    private final boolean kill;

    public HitMarkerS2CPacket(boolean kill) {
        this.kill = kill;
    }

    public HitMarkerS2CPacket(FriendlyByteBuf buf) {
        this.kill = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(kill);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientHitMarker.onHit(kill)
                )
        );
        context.setPacketHandled(true);
    }
}
