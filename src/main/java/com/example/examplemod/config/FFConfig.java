package com.example.examplemod.config;

import net.minecraftforge.common.ForgeConfigSpec;

// 실시간 조정용 설정 (config/examplemod-common.toml)
// 게임을 켠 채로 파일을 수정·저장하면 Forge가 몇 초 안에 자동으로 다시 읽는다 — 재시작·F3+T 불필요.
// 새로 튜닝하고 싶은 수치가 생기면 여기에 항목을 추가하고 코드에서 .get()으로 읽으면 된다.
public class FFConfig {

    public static final ForgeConfigSpec SPEC;

    // ===== 갬블러 데미지 숫자 팝업 =====
    public static final ForgeConfigSpec.DoubleValue POPUP_HEIGHT;     // 머리 위 시작 높이 (블록)
    public static final ForgeConfigSpec.DoubleValue POPUP_SCALE;      // 글자 크기 배율
    public static final ForgeConfigSpec.IntValue POPUP_LIFETIME;      // 지속 (틱, 20 = 1초)
    public static final ForgeConfigSpec.DoubleValue POPUP_RISE;       // 틱당 떠오르는 속도 (블록)

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("damage_popup");
        POPUP_HEIGHT = builder
                .comment("데미지 숫자가 생기는 높이: 대상 키 + 이 값 (블록)")
                .defineInRange("height", 0.35, -1.0, 5.0);
        POPUP_SCALE = builder
                .comment("숫자 크기 배율 (1.0 = 기본)")
                .defineInRange("scale", 1.8, 0.2, 10.0);
        POPUP_LIFETIME = builder
                .comment("숫자 지속 시간 (틱, 20 = 1초)")
                .defineInRange("lifetime_ticks", 22, 5, 100);
        POPUP_RISE = builder
                .comment("틱당 떠오르는 속도 (블록)")
                .defineInRange("rise_per_tick", 0.045, 0.0, 0.5);
        builder.pop();

        SPEC = builder.build();
    }
}
