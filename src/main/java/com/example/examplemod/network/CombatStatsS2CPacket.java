package com.example.examplemod.network;

import com.example.examplemod.client.ClientCombatData;
import com.example.examplemod.combat.CharacterType;
import com.example.examplemod.combat.CoinType;
import com.example.examplemod.combat.SkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class CombatStatsS2CPacket {

    private final int stamina;
    private final int maxStamina;
    private final int ultimateGauge;
    private final CharacterType copiedCharacter;   // NONE = 복사 없음
    @Nullable
    private final SkillType copiedSlot;            // null = 복사 없음
    private final CharacterType character;
    private final CoinType coin0;   // 도박사 코인 슬롯 3개
    private final CoinType coin1;
    private final CoinType coin2;
    private final int nextGunDamage;   // 리볼버 다음 탄환 데미지

    public CombatStatsS2CPacket(int stamina, int maxStamina, int ultimateGauge,
                                CharacterType copiedCharacter, @Nullable SkillType copiedSlot,
                                CharacterType character,
                                CoinType coin0, CoinType coin1, CoinType coin2,
                                int nextGunDamage) {
        this.stamina = stamina;
        this.maxStamina = maxStamina;
        this.ultimateGauge = ultimateGauge;
        this.copiedCharacter = copiedCharacter;
        this.copiedSlot = copiedSlot;
        this.character = character;
        this.coin0 = coin0;
        this.coin1 = coin1;
        this.coin2 = coin2;
        this.nextGunDamage = nextGunDamage;
    }

    public CombatStatsS2CPacket(FriendlyByteBuf buf) {
        this.stamina = buf.readInt();
        this.maxStamina = buf.readInt();
        this.ultimateGauge = buf.readInt();
        this.copiedCharacter = buf.readEnum(CharacterType.class);
        this.copiedSlot = buf.readBoolean() ? buf.readEnum(SkillType.class) : null;
        this.character = buf.readEnum(CharacterType.class);
        this.coin0 = buf.readEnum(CoinType.class);
        this.coin1 = buf.readEnum(CoinType.class);
        this.coin2 = buf.readEnum(CoinType.class);
        this.nextGunDamage = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(stamina);
        buf.writeInt(maxStamina);
        buf.writeInt(ultimateGauge);
        // 문자열 3개 대신 enum(각 1바이트) 전송 — 표시 문자열은 클라가 계산
        buf.writeEnum(copiedCharacter);
        buf.writeBoolean(copiedSlot != null);
        if (copiedSlot != null) {
            buf.writeEnum(copiedSlot);
        }
        buf.writeEnum(character);
        buf.writeEnum(coin0);
        buf.writeEnum(coin1);
        buf.writeEnum(coin2);
        buf.writeInt(nextGunDamage);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() ->
                // 클라이언트에서만 실행해서 저장통 갱신
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientCombatData.set(stamina, maxStamina, ultimateGauge,
                                copiedCharacter, copiedSlot, character, coin0, coin1, coin2, nextGunDamage)
                )
        );

        context.setPacketHandled(true);
    }
}
