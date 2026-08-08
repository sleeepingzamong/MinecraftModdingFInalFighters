package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라: "너 시스템 해킹당함" — durationTicks 동안 이동키 반전 + 화면 글리치
// durationTicks = 0 이면 즉시 해제 (사망·로그아웃 등)
public class SystemHackS2CPacket {

    private final int durationTicks;

    public SystemHackS2CPacket(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public SystemHackS2CPacket(FriendlyByteBuf buf) {
        this.durationTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(durationTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientHackState.set(durationTicks)
                )
        );
        context.setPacketHandled(true);
    }
}
