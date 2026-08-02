package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라: 검사 패링 표식 개수 동기화 (머리 위 표식 렌더링용)
public class ParryMarksS2CPacket {

    private final int entityId;
    private final int marks;

    public ParryMarksS2CPacket(int entityId, int marks) {
        this.entityId = entityId;
        this.marks = marks;
    }

    public ParryMarksS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.marks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(marks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientParryMarks.set(entityId, marks)
                )
        );
        context.setPacketHandled(true);
    }
}
