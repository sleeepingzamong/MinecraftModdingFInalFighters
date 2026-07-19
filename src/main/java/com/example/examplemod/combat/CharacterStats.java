package com.example.examplemod.combat;

public class CharacterStats {

    public static double getMaxHealth(CharacterType characterType) {
        return switch (characterType) {
            case MONKEY -> 24.0D;
            case CYBORG -> 30.0D;
            default -> 20.0D;
        };
    }
    public static int getMaxStamina(CharacterType characterType) {
    return switch (characterType) {
        case MONKEY -> 7;
        case CYBORG -> 1000;
        case HAMMER -> 1000;
        default -> 5;
    };
}

    // 스킬 쿨타임 (틱, 1초 = 20틱). 캐릭터 추가/조절은 여기 숫자만 바꾸면 됨.
    public static int getSkillCooldown(CharacterType characterType, SkillType skillType) {
        if (skillType == SkillType.ULTIMATE) {
            return 0;   // 궁극기는 전부 쿨타임 없음 (게이지가 제한)
        }
        return switch (characterType) {
            case MONKEY -> 10;   // z·x 0.5초
            case CYBORG -> 20;   // z·x 1초
            default -> 0;
        };
    }
}