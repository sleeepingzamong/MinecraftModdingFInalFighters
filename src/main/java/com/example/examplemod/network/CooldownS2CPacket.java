package com.example.examplemod.network;

import com.example.examplemod.combat.SkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 서버 → 클라: 스킬 쿨타임 시작 (HUD 카운트다운용 — 클라가 자체적으로 틱을 센다)
public class CooldownS2CPacket {

    private final SkillType slot;
    private final int ticks;

    public CooldownS2CPacket(SkillType slot, int ticks) {
        this.slot = slot;
        this.ticks = ticks;
    }

    public CooldownS2CPacket(FriendlyByteBuf buf) {
        this.slot = buf.readEnum(SkillType.class);
        this.ticks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(slot);
        buf.writeInt(ticks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.example.examplemod.client.ClientSkillCooldowns.set(slot, ticks)
                )
        );
        context.setPacketHandled(true);
    }
}
