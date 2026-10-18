package com.example.examplemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 주변 클라 전원: 데미지·회복 숫자 팝업 (렌더는 각 클라이언트가 지형 단계에서 직접 그림 —
// 블록에는 가려지고, 분신·부처·빔 같은 엔티티에는 안 가려짐)
public class DamagePopupS2CPacket {

    // 숫자 종류 (색): 기본 피해 = 금색, 헤드샷 = 빨강, 회복 = 초록 "+숫자"
    public static final byte DAMAGE = 0;
    public static final byte HEADSHOT = 1;
    public static final byte HEAL = 2;

    private final double x;
    private final double y;
    private final double z;
    private final float amount;
    private final byte kind;

    public DamagePopupS2CPacket(double x, double y, double z, float amount, byte kind) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.amount = amount;
        this.kind = kind;
    }

    public DamagePopupS2CPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.amount = buf.readFloat();
        this.kind = buf.readByte();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(amount);
        buf.writeByte(kind);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientDamagePopups.add(x, y, z, amount, kind)
                )
        );
        context.setPacketHandled(true);
    }
}
