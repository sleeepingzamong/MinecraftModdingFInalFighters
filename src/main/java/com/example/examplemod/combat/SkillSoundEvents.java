package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

// 캐릭터별 평타 사운드 슬롯 재생 — 기본은 무음.
// sounds/skill/<캐릭터>_attack.ogg 파일만 교체하면 재시작 없이 F3+T로 새 소리 적용.
// (LOWEST 우선순위 = 가드·회전 등으로 취소된 공격에는 재생 안 됨)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class SkillSoundEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CharacterType character = PlayerCharacterManager.getCharacter(player);
        if (character == CharacterType.NONE) {
            return;
        }
        var sound = ModSounds.SKILL_SOUNDS.get(
                character.name().toLowerCase(Locale.ROOT) + "_attack");
        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
