package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 모드 전용 상태를 화면 오른쪽 위(바닐라 물약 HUD)에 아이콘으로 보여주기 위한 "표시 전용" 효과들.
// 실제 로직은 각 매니저(StunManager, BerserkerEvents 등)가 처리하고, 여기 효과는 아이콘·지속시간 표시만 담당.
// 아이콘: assets/examplemod/textures/mob_effect/<이름>.png (18x18)
public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ExampleMod.MODID);

    // 파티클·속성 없이 아이콘만 표시하는 효과
    private static class MarkerEffect extends MobEffect {
        MarkerEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    public static final RegistryObject<MobEffect> STUNNED = EFFECTS.register("stunned",
            () -> new MarkerEffect(MobEffectCategory.HARMFUL, 0xF5D442));       // 스턴
    public static final RegistryObject<MobEffect> FRENZY = EFFECTS.register("frenzy",
            () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xC81E28));    // 광전사 X 격노
    public static final RegistryObject<MobEffect> RAMPAGE = EFFECTS.register("rampage",
            () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xFF3A1E));    // 광전사 궁 광폭화
    public static final RegistryObject<MobEffect> DOUBLE_HIT = EFFECTS.register("double_hit",
            () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x7FD0FF));    // 검사 Z 연격 (2타 피해)
    public static final RegistryObject<MobEffect> PARRY = EFFECTS.register("parry",
            () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xA8D8FF));    // 검사 X 패링
    public static final RegistryObject<MobEffect> ENHANCE = EFFECTS.register("enhance",
            () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x4080FF));    // 검사 궁 강화
    public static final RegistryObject<MobEffect> BUG_IGNORE = EFFECTS.register("bug_ignore",
            () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x39E060));    // 해커 X 버그 무시
    public static final RegistryObject<MobEffect> HACKED = EFFECTS.register("hacked",
            () -> new MarkerEffect(MobEffectCategory.HARMFUL, 0xFF00E0));       // 해커 궁에 당함 (키 반전)
    public static final RegistryObject<MobEffect> COIN_SEVEN = EFFECTS.register("coin_seven",
            () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xFFC94A));    // 갬블러 코인 7 (+15%/스택)
    public static final RegistryObject<MobEffect> COIN_SKULL = EFFECTS.register("coin_skull",
            () -> new MarkerEffect(MobEffectCategory.HARMFUL, 0x8A2C34));       // 갬블러 코인 해골 (-15%/스택)
    public static final RegistryObject<MobEffect> JACKPOT = EFFECTS.register("jackpot",
            () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xFFD700));    // 룰렛 7 우세 (+40%)
    public static final RegistryObject<MobEffect> CURSED = EFFECTS.register("cursed",
            () -> new MarkerEffect(MobEffectCategory.HARMFUL, 0xB01828));       // 룰렛 해골 우세 (-50%, 대상)
    public static final RegistryObject<MobEffect> BURN = EFFECTS.register("burn",
            () -> new MarkerEffect(MobEffectCategory.HARMFUL, 0xF07818));       // 불사조 화상 (초당 1)

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
