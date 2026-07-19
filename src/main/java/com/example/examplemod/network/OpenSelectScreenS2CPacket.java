package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라: 캐릭터 선택창을 열어라 (커맨드 블록/서버 커맨드용)
// 내용물 없는 신호용 패킷. 받은 클라이언트는 자기 화면에 선택창을 띄운다.
public class OpenSelectScreenS2CPacket {

    public OpenSelectScreenS2CPacket() {
    }

    public OpenSelectScreenS2CPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientHooks.openCharacterSelectScreen()
                )
        );
        context.setPacketHandled(true);
    }
}
