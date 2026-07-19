package com.example.examplemod.combat;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.entity.CloneEntity;
import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.registry.ModItems;
import com.example.examplemod.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class CyborgSkills implements CharacterSkills {

    // 복사 보관함에 스킬이 있는지 (SkillManager가 자원 소모 전에 확인)
    public static boolean hasCopiedSkill(ServerPlayer player) {
        return player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(d -> d.getCopiedCharacter() != CharacterType.NONE && d.getCopiedSlot() != null)
                .orElse(false);
    }

    @Override
    public void useSkill1(ServerPlayer player, int chargeTick) {
        // Z 스킬: 마지막에 맞은 투사체 스킬을 +10% 데미지로 되돌려준 뒤 소모
        // (복사 없음 체크는 SkillManager가 스태미나 소모 전에 함)
        CharacterType copied = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(PlayerCombatData::getCopiedCharacter).orElse(CharacterType.NONE);
        SkillType slot = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(PlayerCombatData::getCopiedSlot).orElse(null);

        if (copied == CharacterType.NONE || slot == null) {
            return;   // 안전장치 (정상 흐름에선 도달 안 함)
        }

        SkillManager.castCopiedSkill(player, copied, slot, chargeTick);

        // 소모: 보관함 비우기
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            data.setCopiedCharacter(CharacterType.NONE);
            data.setCopiedSlot(null);
        });
        CombatSync.syncStats(player);   // HUD 복사 스킬 표시 → 없음
    }

    @Override
    public void useSkill2(ServerPlayer player, int chargeTick) {
        // 분신 소환: 플레이어 스킨 분신이 10초간 주변 적대 몹을 근접 공격한다.
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        CloneEntity clone = ModEntities.CLONE.get().create(level); // @Nullable
        if (clone == null) {
            return;
        }

        // 플레이어 위치/방향에 배치한다.
        Vec3 pos = player.position();
        clone.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
        clone.setYBodyRot(player.getYRot());

        // 클라이언트 렌더러가 스킨을 찾는 데 쓰는 소환자 UUID (서버에서 미리 set, sync됨).
        clone.setOwnerUUID(player.getUUID());

        // 손에 카타나 쥐여주기 (죽어도 안 떨구게 드롭 확률 0)
        clone.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KATANA.get()));
        clone.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        level.addFreshEntity(clone);

        // player.sendSystemMessage(Component.literal("사이보그 스킬 2 - 분신 소환! 차징: " + chargeTick + "틱"));
    }

    @Override
    public void useUltimate(ServerPlayer player, int chargeTick) {
        // 궁극기: 효과음(모두에게) → 1초 충전 → 3초 레이저
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // 접속한 모든 플레이어에게 각자 위치에서 재생 → 거리 무관하게 다 들림
        long seed = level.getRandom().nextLong();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSoundPacket(
                    Holder.direct(ModSounds.LASER_CHARGE.get()), SoundSource.PLAYERS,
                    p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F, seed));
        }

        // 2초(40틱) 충전 후 3초(60틱) 레이저
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            data.setLaserChargeTicks(40);
            data.setLaserTicksLeft(60);
        });

        // 시전자 클라에 반동 연출 시작 알림 (충전 40틱 + 발사 60틱)
        com.example.examplemod.network.ModMessages.sendToPlayer(
                new com.example.examplemod.network.LaserStateS2CPacket(40, 60), player);
    }
}
