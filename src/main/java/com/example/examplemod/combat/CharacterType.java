package com.example.examplemod.combat;
public enum CharacterType {
    // 괄호 안 = 화면·채팅에 보일 한글 이름.
    // 캐릭터 추가 시 여기 한 줄만 넣으면 선택창 버튼/선택 메시지에 자동 반영됨.
    NONE("선택 안 됨"),
    MONKEY("원숭이"),
    CYBORG("사이보그"),
    DOJUK("도적"),
    GAMBLER("갬블러"),
    SAMURAI("사무라이"),
    HACKER("해커"),
    HAMMER("해머"),
    SINGER("가수");

    private final String displayName;

    CharacterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}