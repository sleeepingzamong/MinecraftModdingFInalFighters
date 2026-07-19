package com.example.examplemod.network;

import com.example.examplemod.combat.CharacterType;
import com.example.examplemod.combat.PlayerCharacterManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectCharacterC2SPacket {

    private final CharacterType characterType;

    public SelectCharacterC2SPacket(CharacterType characterType) {
        this.characterType = characterType;
    }

    public SelectCharacterC2SPacket(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        CharacterType parsed;
        try {
            parsed = CharacterType.valueOf(name);
        } catch (IllegalArgumentException e) {
            parsed = CharacterType.NONE;
        }
        this.characterType = parsed;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(characterType.name());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                PlayerCharacterManager.setCharacter(player, characterType);

                player.sendSystemMessage(Component.literal(
                        "캐릭터가 " + characterType.getDisplayName() + "으로 선택되었습니다."
                ));
            }
        });

        context.setPacketHandled(true);
    }
}