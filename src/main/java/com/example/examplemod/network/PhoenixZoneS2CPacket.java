package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 차원 안 클라 전원: 불사조 궁 장판 바닥 문양 표시 / 제거 (duration 0 = 제거)
public class PhoenixZoneS2CPacket {

    private final int id;
    private final double x;
    private final double y;
    private final double z;
    private final float radius;
    private final int duration;
    private final float yaw;   // 시전자가 바라보던 방향 — 문양 속 불사조 머리 방향

    public PhoenixZoneS2CPacket(int id, double x, double y, double z, float radius, int duration, float yaw) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.duration = duration;
        this.yaw = yaw;
    }

    public static PhoenixZoneS2CPacket remove(int id) {
        return new PhoenixZoneS2CPacket(id, 0, 0, 0, 0, 0, 0);
    }

    public PhoenixZoneS2CPacket(FriendlyByteBuf buf) {
        this.id = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.radius = buf.readFloat();
        this.duration = buf.readInt();
        this.yaw = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(radius);
        buf.writeInt(duration);
        buf.writeFloat(yaw);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    if (duration <= 0) {
                        com.example.examplemod.client.ClientPhoenixZones.remove(id);
                    } else {
                        com.example.examplemod.client.ClientPhoenixZones.add(id, x, y, z, radius, duration, yaw);
                    }
                })
        );
        context.setPacketHandled(true);
    }
}
