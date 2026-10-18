package com.example.examplemod.combat;

public class CharacterStats {

    // 역할별 체력 계단 (2포인트 단위): 탱커 26 > 브루저 22 > 기준 20 > 원거리·교란 18 > 암살자 16
    public static double getMaxHealth(CharacterType characterType) {
        return switch (characterType) {
            case CYBORG -> 26.0D;      // 탱커 — 분신·레이저 보유
            case HAMMER -> 24.0D;      // CC 탱커 — 최저 DPS 보상
            case MONKEY -> 22.0D;      // 리치 브루저 — 최장 사거리(5.0)의 대가
            case BERSERKER -> 22.0D;   // 리스크 브루저 — 분노·궁 피해감소 20%가 실질 탱킹
            case GAMBLER -> 18.0D;     // 원거리 — 거리 유지가 생명줄
            case HACKER -> 18.0D;      // 교란형 — CC가 강한 대신 몸이 약함
            case DOJUK -> 18.0D;       // 암살자 — 최고 DPS·은신·기동이지만 16은 너무 물몸이라 상향
            case PHOENIX -> 16.0D;     // 서포터 — 최저 체력, 궁 부활(목숨 2개)이 실질 탱킹
            default -> 20.0D;          // 검사 = 기준점 (패링·강화 보유)
        };
    }
    // 스태미나 (스킬 1·대쉬 1 소모, 1.75초당 1 회복): 기동형일수록 많고 탱커일수록 적음
    public static int getMaxStamina(CharacterType characterType) {
        return switch (characterType) {
            case MONKEY -> 8;       // 스킬 스팸을 스태미나로 제한하는 컨셉 (쿨 0.5초 대신 자원 압박)
            case CYBORG -> 8;       // 탱커 — 기동 자원 적음
            case DOJUK -> 12;       // 암살자 — 대쉬 의존이 가장 큼
            case SWORDSMAN -> 8;    // 듀얼리스트 — 연격·패링이 강해 자원은 타이트하게
            case BERSERKER -> 8;    // 브루저 — Z 돌진은 자체 쿨타임 있음
            case HAMMER -> 10;      // 회전(초당 1 소모+회복 정지)이 주력이라 자원 여유 필요
            case PHOENIX -> 6;      // 서포터 — Z 연사(0.75초 쿨)를 스태미나가 제한하는 설계
            default -> 10;          // 갬블러·해커 기준
        };
    }

    // 캐릭터 고유 방어력 (장비 없이 능력치로 부여 — 전용 방어구가 없는 캐릭터용)
    // 불사조는 이제 전용 방어구(불사조 날개, 방어력 2)가 생겨서 여기서 제외 (중복 방지)
    public static double getBonusArmor(CharacterType characterType) {
        return switch (characterType) {
            default -> 0.0D;
        };
    }

    // 스킬 쿨타임 (틱, 1초 = 20틱). 캐릭터 추가/조절은 여기 숫자만 바꾸면 됨.
    public static int getSkillCooldown(CharacterType characterType, SkillType skillType) {
        if (skillType == SkillType.ULTIMATE) {
            return 0;   // 궁극기는 전부 쿨타임 없음 (게이지가 제한)
        }
        return switch (characterType) {
            case MONKEY -> 70;    // z·x 3.5초
            case CYBORG -> 20;    // z·x 1초
            case DOJUK -> skillType == SkillType.SKILL_1 ? 40 : 0;    // Z 순간이동 2초 (X는 표창→돌진 재입력이라 자체 관리)
            case GAMBLER -> skillType == SkillType.SKILL_1 ? 20 : 60; // Z 코인 1초, X 카드 3초
            case HAMMER -> skillType == SkillType.SKILL_1 ? 100 : 0;  // Z 투척 5초 (X는 자체 쿨 관리)
            case SWORDSMAN -> skillType == SkillType.SKILL_1 ? 180 : 200;  // Z 연격 9초, X 패링 10초
            case HACKER -> skillType == SkillType.SKILL_1 ? 100 : 200;     // Z 롤백 5초, X 버그무시 10초
            case BERSERKER -> skillType == SkillType.SKILL_1 ? 80 : 200;  // Z 돌진 4초, X 격노 10초
            case PHOENIX -> skillType == SkillType.SKILL_1 ? 15 : 0;      // Z 깃털 0.75초 (X는 착지 후 자체 관리)
            default -> 0;
        };
    }
}