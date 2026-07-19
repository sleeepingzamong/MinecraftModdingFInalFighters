package com.example.examplemod.network;

import com.example.examplemod.client.ClientCombatData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 레이저 궁극기 시전 알림 (시전자에게): 클라가 충전/발사 구간을 알고 반동을 연출
public class LaserStateS2CPacket {

    private final int chargeTicks;
    private final int beamTicks;

    public LaserStateS2CPacket(int chargeTicks, int beamTicks) {
        this.chargeTicks = chargeTicks;
        this.beamTicks = beamTicks;
    }

    public LaserStateS2CPacket(FriendlyByteBuf buf) {
        this.chargeTicks = buf.readInt();
        this.beamTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(chargeTicks);
        buf.writeInt(beamTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientCombatData.startLaserState(chargeTicks, beamTicks)
                )
        );
        context.setPacketHandled(true);
    }
}
