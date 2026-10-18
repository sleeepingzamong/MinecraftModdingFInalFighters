package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라: 광전사 분노 스택 동기화 (본인 HUD 표시용)
public class RageS2CPacket {

    private final int stacks;

    public RageS2CPacket(int stacks) {
        this.stacks = stacks;
    }

    public RageS2CPacket(FriendlyByteBuf buf) {
        this.stacks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(stacks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientRageData.set(stacks)
                )
        );
        context.setPacketHandled(true);
    }
}
