package com.example.examplemod.network;

import com.example.examplemod.combat.SkillManager;
import com.example.examplemod.combat.SkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UseSkillC2SPacket {

    private final SkillType skillType;
    private final int chargeTick;

    public UseSkillC2SPacket(SkillType skillType, int chargeTick) {
        this.skillType = skillType;
        this.chargeTick = chargeTick;
    }

    public UseSkillC2SPacket(SkillType skillType) {
        this(skillType, 0);
    }

    public UseSkillC2SPacket(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        SkillType parsed;
        try {
            parsed = SkillType.valueOf(name);
        } catch (IllegalArgumentException e) {
            parsed = null;
        }
        this.skillType = parsed;
        this.chargeTick = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillType.name());
        buf.writeInt(chargeTick);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null && skillType != null) {
                SkillManager.useSkill(player, skillType, chargeTick);
            }
        });

        context.setPacketHandled(true);
    }
}