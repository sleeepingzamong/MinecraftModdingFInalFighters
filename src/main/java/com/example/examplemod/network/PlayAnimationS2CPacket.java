package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라: 특정 플레이어에게 플레이어 애니메이션(1회성)을 재생하라
// animName = player_animation 폴더의 애니메이션 이름 (예: "hammer_ultimate")
public class PlayAnimationS2CPacket {

    private final int playerId;
    private final String animName;

    public PlayAnimationS2CPacket(int playerId, String animName) {
        this.playerId = playerId;
        this.animName = animName;
    }

    public PlayAnimationS2CPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.animName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeUtf(animName);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.PlayerAnimations.playCastAnimation(playerId, animName)
                )
        );
        context.setPacketHandled(true);
    }
}
