package com.example.examplemod.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.example.examplemod.capability.PlayerCombatDataProvider;   // 맨 위 import 구역에
public class SkillManager {

    // 동시 시전 방지: 마지막 스킬 시전 시각 (이 안에 들어온 두 번째 스킬은 무시)
    private static final java.util.Map<java.util.UUID, Long> LAST_CAST_TICK = new java.util.HashMap<>();
    private static final int SIMULCAST_WINDOW = 3;   // 3틱(0.15초)

    private static final CharacterSkills MONKEY_SKILLS = new MonkeySkills();
    private static final CharacterSkills CYBORG_SKILLS = new CyborgSkills();
    private static final CharacterSkills DOJUK_SKILLS = new DojukSkills();
    private static final CharacterSkills GAMBLER_SKILLS = new GamblerSkills();
    private static final CharacterSkills SWORDSMAN_SKILLS = new SwordsmanSkills();
    private static final CharacterSkills HACKER_SKILLS = new HackerSkills();
    private static final CharacterSkills HAMMER_SKILLS = new HammerSkills();
    private static final CharacterSkills BERSERKER_SKILLS = new BerserkerSkills();
    private static final CharacterSkills PHOENIX_SKILLS = new PhoenixSkills();

    public static void useSkill(ServerPlayer player, SkillType skillType, int chargeTick) {
        if (GuardManager.isGuarding(player)) {
            player.sendSystemMessage(Component.literal("방어 중에는 행동할 수 없습니다."));
            return;
        }

        if (GamblerEvents.isRouletteLocked(player)) {
            return;   // 룰렛 연출 중엔 스킬 불가
        }

        if (StunManager.isStunned(player)) {
            player.sendSystemMessage(Component.literal("스턴 상태입니다!"));
            return;
        }

        if (HammerUltimateEvents.isCasting(player)) {
            return;   // 해머 궁 시전 경직 중 — 다른 스킬 불가
        }

        CharacterType characterType = PlayerCharacterManager.getCharacter(player);
        CharacterSkills skills = getSkills(characterType);

        // 캐릭터 미선택 — 자원 소모 전에 차단
        if (skills == null) {
            player.sendSystemMessage(Component.literal("능력 없음"));
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

        // 해머 Z(투척)·X(회전)는 망치를 들고 있어야 함 (자원 소모 전에 차단)
        if (characterType == CharacterType.HAMMER
                && (skillType == SkillType.SKILL_1 || skillType == SkillType.SKILL_2)
                && !(player.getMainHandItem().getItem() instanceof com.example.examplemod.item.HammerItem)) {
            player.sendSystemMessage(Component.literal("망치를 들어야 합니다."));
            return;
        }

        // 스킬 쿨타임 체크 (소모 전 → 쿨 중엔 스태미나·게이지 안 닳음)
        long now = player.level().getGameTime();
        boolean onCooldown = player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .map(data -> now < data.getSkillReadyTick(skillType))
                .orElse(false);
        // 도적 X: 표식·돌진 중의 재입력(돌진 시작·취소)은 표창 쿨타임을 무시
        boolean bypassCooldown = characterType == CharacterType.DOJUK && skillType == SkillType.SKILL_2
                && DojukSkills.isMarkOrDashActive(player);
        if (onCooldown && !bypassCooldown) {
            return;   // 대쉬처럼 조용히 무시
        }

        // 두 스킬 키를 동시에 눌러 같은 순간 겹쳐 시전되는 것 방지 (자원 소모 전 차단)
        Long lastCast = LAST_CAST_TICK.get(player.getUUID());
        if (lastCast != null && now - lastCast < SIMULCAST_WINDOW) {
            return;
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

        // 도적 Z: 시선에 대상이 없으면 자원 소모 전에 차단 (스태미나·쿨타임 안 씀)
        if (characterType == CharacterType.DOJUK && skillType == SkillType.SKILL_1
                && !DojukSkills.hasTeleportTarget(player)) {
            player.sendSystemMessage(Component.literal("대상이 없습니다."));
            return;
        }

        // 사이보그 X: 분신 2개 제한 (자원 소모 전에 차단)
        if (characterType == CharacterType.CYBORG && skillType == SkillType.SKILL_2
                && !CyborgSkills.canSummonClone(player)) {
            player.sendSystemMessage(Component.literal("분신은 2개까지만 소환할 수 있습니다."));
            return;
        }

        // 광전사 X: 분노가 부족하면 자원 소모 전에 차단 (스태미나·쿨타임 안 씀)
        if (characterType == CharacterType.BERSERKER && skillType == SkillType.SKILL_2
                && !BerserkerEvents.hasFrenzyRage(player)) {
            player.sendSystemMessage(Component.literal(
                    "분노가 부족합니다! (" + BerserkerEvents.FRENZY_RAGE_COST + " 필요)"));
            return;
        }

        // 불사조 X: 공중(미착지) 상태에서는 재사용 불가 (착지해야 쿨타임이 돌기 시작)
        if (characterType == CharacterType.PHOENIX && skillType == SkillType.SKILL_2
                && PhoenixEvents.isDashPending(player)) {
            return;
        }


        // 갬블러 궁: 대상 없음·룰렛 진행 중이면 게이지 소모 전에 차단
        if (characterType == CharacterType.GAMBLER && skillType == SkillType.ULTIMATE
                && !GamblerSkills.canStartRoulette(player)) {
            player.sendSystemMessage(Component.literal("대상이 없습니다."));
            return;
        }

        // 해커 궁: 시선에 플레이어가 없으면 게이지 소모 전에 차단
        if (characterType == CharacterType.HACKER && skillType == SkillType.ULTIMATE
                && !HackerEvents.hasHackTarget(player)) {
            player.sendSystemMessage(Component.literal("대상(플레이어)이 없습니다."));
            return;
        }

        if (skillType == SkillType.ULTIMATE) {
            // 궁극기는 게이지 전체(100)를 소모 (스태미나는 안 씀)
            if (!UltimateGaugeManager.consumeGauge(player, UltimateGaugeManager.MAX_GAUGE)) {
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

        LAST_CAST_TICK.put(player.getUUID(), now);

        switch (skillType) {
            case SKILL_1 -> skills.useSkill1(player, chargeTick);
            case SKILL_2 -> skills.useSkill2(player, chargeTick);
            case ULTIMATE -> skills.useUltimate(player, chargeTick);
            default -> {
            }
        }

        // 캐릭터별 스킬 사운드 슬롯 재생 (기본은 무음 — sounds/skill/<캐릭터>_<z|x|ult>.ogg만
        // 교체하면 재시작 없이 F3+T로 새 소리가 적용됨)
        String slotKey = switch (skillType) {
            case SKILL_1 -> "z";
            case SKILL_2 -> "x";
            case ULTIMATE -> "ult";
            default -> null;
        };
        if (slotKey != null) {
            var slotSound = com.example.examplemod.registry.ModSounds.SKILL_SOUNDS.get(
                    characterType.name().toLowerCase(java.util.Locale.ROOT) + "_" + slotKey);
            if (slotSound != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        slotSound.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        // 시전 성공 → 쿨타임 시작 (+ HUD 카운트다운용으로 클라에 알림)
        int cooldown = CharacterStats.getSkillCooldown(characterType, skillType);
        if (cooldown > 0) {
            final long readyAt = now + cooldown;
            player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                    .ifPresent(data -> data.setSkillReadyTick(skillType, readyAt));
            com.example.examplemod.network.ModMessages.sendToPlayer(
                    new com.example.examplemod.network.CooldownS2CPacket(skillType, cooldown), player);
        }
    }

    // 외부에서 쿨타임을 강제로 걸기 (캐릭터 선택·상태초기화 시 해커 Z 등) — HUD 카운트다운 포함
    public static void applyCooldown(ServerPlayer player, SkillType skillType, int ticks) {
        if (ticks <= 0) {
            return;
        }
        final long readyAt = player.level().getGameTime() + ticks;
        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA)
                .ifPresent(data -> data.setSkillReadyTick(skillType, readyAt));
        com.example.examplemod.network.ModMessages.sendToPlayer(
                new com.example.examplemod.network.CooldownS2CPacket(skillType, ticks), player);
    }

    public static void useDash(ServerPlayer player, double dirX, double dirZ) {
    if (GuardManager.isGuarding(player)) {
        player.sendSystemMessage(Component.literal("방어 중에는 행동할 수 없습니다."));
        return;
    }

    if (StunManager.isStunned(player)) {
        return;   // 스턴 중엔 대쉬 불가
    }

    if (HammerUltimateEvents.isCasting(player)) {
        return;   // 해머 궁 시전 경직 중엔 대쉬 불가
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
            case SWORDSMAN -> SWORDSMAN_SKILLS;
            case HACKER -> HACKER_SKILLS;
            case HAMMER -> HAMMER_SKILLS;
            case BERSERKER -> BERSERKER_SKILLS;
            case PHOENIX -> PHOENIX_SKILLS;
            default -> null;
        };
    }
}