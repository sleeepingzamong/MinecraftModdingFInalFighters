package com.example.examplemod.network;

import com.example.examplemod.item.RevolverItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 리볼버 발사 요청 (좌클릭 시 클라 → 서버). 내용물 없음 — 검증은 전부 서버가 함.
public class GunShootC2SPacket {

    public GunShootC2SPacket() {
    }

    public GunShootC2SPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            // 관전 중·스턴 중 발사 불가 (리볼버 사격 = 갬블러 평타)
            if (player != null && !player.isSpectator()
                    && !com.example.examplemod.combat.StunManager.isStunned(player)) {
                RevolverItem.tryShoot(player);
            }
        });
        context.setPacketHandled(true);
    }
}
