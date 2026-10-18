package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라(가드한 본인): 가드로 피해를 막아냄 → 파란 비네트 플래시 + 방패 아이콘 펄스
public class GuardBlockS2CPacket {

    public GuardBlockS2CPacket() {
    }

    public GuardBlockS2CPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientGuardFx.onBlock()
                )
        );
        context.setPacketHandled(true);
    }
}
