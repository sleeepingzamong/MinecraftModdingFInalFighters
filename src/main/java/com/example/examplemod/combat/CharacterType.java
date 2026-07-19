package com.example.examplemod.combat;
public enum CharacterType {
    // 괄호 안 = 화면·채팅에 보일 한글 이름, 그 뒤 = 선택창 오른쪽에 표시될 설명 줄들.
    // 캐릭터 추가/스킬 변경 시 여기만 고치면 선택창 버튼·설명에 자동 반영됨.
    NONE("선택 안 됨"),
    MONKEY("원숭이",
            "Z: 여의봉 밀치기",
            "X: 여의봉 끌어오기",
            "궁: 여래신장 - 부처 발사"),
    CYBORG("사이보그",
            "Z: 복사한 스킬 사용",
            "X: 분신 소환",
            "궁: 레이저 - 2초 충전 후 발사"),
    DOJUK("도적",
            "Z: 대상 등 뒤로 순간이동",
            "X: 표창 - 명중 시 돌진",
            "궁: 10초 투명화, 기습 강화"),
    GAMBLER("갬블러",
            "Z: 코인 토스 - 공격 ±15%",
            "X: 블랙잭 카드 - 21은 폭발",
            "궁: 룰렛 - 돔에 갇힌 도박"),
    SAMURAI("사무라이",
            "스킬 준비 중"),
    HACKER("해커",
            "스킬 준비 중"),
    HAMMER("해머",
            "X: 회전 베기 - 주변 지속 피해",
            "궁: 준비 중"),
    SINGER("가수",
            "스킬 준비 중");

    private final String displayName;
    private final String[] description;

    CharacterType(String displayName, String... description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getDescription() {
        return description;
    }
}
