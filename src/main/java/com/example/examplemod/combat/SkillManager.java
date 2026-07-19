package com.example.examplemod.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.example.examplemod.capability.PlayerCombatDataProvider;   // 맨 위 import 구역에
public class SkillManager {

    private static final CharacterSkills MONKEY_SKILLS = new MonkeySkills();
    private static final CharacterSkills CYBORG_SKILLS = new CyborgSkills();
    private static final CharacterSkills DOJUK_SKILLS = new DojukSkills();
    private static final CharacterSkills GAMBLER_SKILLS = new GamblerSkills();
    private static final CharacterSkills SAMURAI_SKILLS = new SamuraiSkills();
    private static final CharacterSkills HACKER_SKILLS = new HackerSkills();
    private static final CharacterSkills HAMMER_SKILLS = new HammerSkills();
    private static final CharacterSkills SINGER_SKILLS = new SingerSkills();

    public static void useSkill(ServerPlayer player, SkillType skillType, int chargeTick) {
        if (GuardManager.isGuarding(player)) {
            player.sendSystemMessage(Component.literal("방어 중에는 행동할 수 없습니다."));
            return;
        }

        if (GamblerEvents.isRouletteLocked(player)) {
            return;   // 룰렛 연출 중엔 스킬 불가
        }

        

        CharacterType characterType = PlayerCharacterManager.getCharacter(player);
        CharacterSkills skills = getSkills(characterType);

        if (skills == null) {
            player.sendSystemMessage(Component.literal("캐릭터가 선택되지 않았습니다."));
            return;
        }

        // 해머 X 재입력 = 회전 종료 (자원 소모 없음, 쿨타임보다 먼저 — 언제든 끌 수 있어야 함)
        if (characterType == CharacterType.HAMMER && skillType == SkillType.SKILL_2
                && HammerSpinEvents.toggleOff(player)) {
            return;
        }

        // 회전 베기 중에는 다른 스킬 사용 불가
        if (HammerSpinEvents.isSpinning(player)) {
            player.sendSystemMessage(Component.literal("회전 중에는 사용할 수 없습니다."));
            return;
        }

        // 해머 X는 망치를 들고 있어야 함 (자원 소모 전에 차단)
        if (characterType == CharacterType.HAMMER && skillType == SkillType.SKILL_2
                && !(player.getMainHandItem().getItem() instanceof com.example.examplemod.item.HammerItem)) {
            player.sendSystemMessage(Component.literal("망치를 들어야 합니다."));
            return;
        }

        // 스킬 쿨타임 체크 (소모 전 → 쿨 중엔 스태미나·게이지 안 닳음)
        long now = player.level().getGameTime();
        boolean onCooldown = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(data -> now < data.getSkillReadyTick(skillType))
                .orElse(false);
        if (onCooldown) {
            return;   // 대쉬처럼 조용히 무시
        }

        // 사이보그 Z: 복사한 스킬이 없으면 자원 소모 전에 차단 (스태미나·쿨타임 안 씀)
        if (characterType == CharacterType.CYBORG && skillType == SkillType.SKILL_1
                && !CyborgSkills.hasCopiedSkill(player)) {
            player.sendSystemMessage(Component.literal("복사한 스킬이 없습니다."));
            return;
        }

        // 도박사 Z: 코인 3개가 가득이면 자원 소모 전에 차단
        if (characterType == CharacterType.GAMBLER && skillType == SkillType.SKILL_1
                && !GamblerSkills.hasEmptyCoinSlot(player)) {
            player.sendSystemMessage(Component.literal("코인이 가득 찼습니다."));
            return;
        }

        if (skillType == SkillType.ULTIMATE) {
            // 궁극기는 게이지만 소모 (스태미나는 안 씀)
            if (!UltimateGaugeManager.consumeGauge(player, 1)) {
                return;
            }
        } else if (!StaminaManager.consumeStamina(player, 1)) {
            return;   // 일반 스킬만 스태미나 소모
        }

        // 투사체에 "이 스킬의 출처(캐릭터+슬롯)"를 새기기 위해 시전 정보 기록
        final CharacterType castChar = characterType;
        final SkillType castSlotType = skillType;
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            data.setCastCharacter(castChar);
            data.setCastSlot(castSlotType);
        });

        switch (skillType) {
            case SKILL_1 -> skills.useSkill1(player, chargeTick);
            case SKILL_2 -> skills.useSkill2(player, chargeTick);
            case ULTIMATE -> skills.useUltimate(player, chargeTick);
            default -> {
            }
        }

        // 시전 성공 → 쿨타임 시작
        int cooldown = CharacterStats.getSkillCooldown(characterType, skillType);
        if (cooldown > 0) {
            final long readyAt = now + cooldown;
            player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                    .ifPresent(data -> data.setSkillReadyTick(skillType, readyAt));
        }
    }

    public static void useDash(ServerPlayer player, double dirX, double dirZ) {
    if (GuardManager.isGuarding(player)) {
        player.sendSystemMessage(Component.literal("방어 중에는 행동할 수 없습니다."));
        return;
    }

    if (GamblerEvents.isRouletteLocked(player)) {
        return;   // 룰렛 연출 중엔 대쉬 불가
    }

    if (HammerSpinEvents.isSpinning(player)) {
        return;   // 회전 베기 중엔 대쉬 불가
    }

    // 대쉬 쿨타임 (0.3초 = 6틱)
    long now = player.level().getGameTime();
    boolean onCooldown = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
            .map(data -> now < data.getDashReadyTick())
            .orElse(false);
    if (onCooldown) {
        return;   // 아직 쿨타임 → 조용히 무시
    }

    if (!StaminaManager.consumeStamina(player, 1)) {
        return;
    }
    CommonCombatActions.dash(player, dirX, dirZ);

    // 쿨타임 시작: 지금부터 6틱 뒤에 다시 가능
    final long readyAt = now + 15L;
    player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
            .ifPresent(data -> data.setDashReadyTick(readyAt));
}

    // 복사한 스킬을 +10% 데미지로 시전 (사이보그 Z 스킬이 호출)
    public static void castCopiedSkill(ServerPlayer player, CharacterType source, SkillType slot, int chargeTick) {
        CharacterSkills skills = getSkills(source);
        if (skills == null) {
            return;
        }
        // 시전 출처 = 복사한 스킬로 지정, 데미지 버프 ON
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            data.setCastCharacter(source);
            data.setCastSlot(slot);
            data.setCopyBuffActive(true);
        });

        switch (slot) {
            case SKILL_1 -> skills.useSkill1(player, chargeTick);
            case SKILL_2 -> skills.useSkill2(player, chargeTick);
            default -> {
            }   // 궁극기·대쉬는 복사 대상 아님
        }

        // 버프 OFF — 이후 일반 공격엔 영향 없도록
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data ->
                data.setCopyBuffActive(false));
    }

    private static CharacterSkills getSkills(CharacterType characterType) {
        return switch (characterType) {
            case MONKEY -> MONKEY_SKILLS;
            case CYBORG -> CYBORG_SKILLS;
            case DOJUK -> DOJUK_SKILLS;
            case GAMBLER -> GAMBLER_SKILLS;
            case SAMURAI -> SAMURAI_SKILLS;
            case HACKER -> HACKER_SKILLS;
            case HAMMER -> HAMMER_SKILLS;
            case SINGER -> SINGER_SKILLS;
            default -> null;
        };
    }
}