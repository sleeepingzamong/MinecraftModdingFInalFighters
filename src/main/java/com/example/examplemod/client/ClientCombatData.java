package com.example.examplemod.client;

import com.example.examplemod.combat.CharacterType;
import com.example.examplemod.combat.CoinType;
import com.example.examplemod.combat.SkillType;

import javax.annotation.Nullable;
import java.util.Locale;

public class ClientCombatData {

    private static int stamina = 0;
    private static int maxStamina = 0;
    private static int ultimateGauge = 0;
    private static String copiedSkill = "없음";
    private static String copiedSkillId = "none";
    private static String character = "NONE";
    private static final CoinType[] coins = {CoinType.NONE, CoinType.NONE, CoinType.NONE};   // 도박사 코인 3슬롯
    private static int nextGunDamage = 0;   // 리볼버 다음 탄환 데미지

    // 패킷이 도착하면 이걸로 값을 갱신 (enum을 받아 표시 문자열은 여기서 계산 — 패킷당 1회뿐)
    public static void set(int stamina, int maxStamina, int ultimateGauge,
                           CharacterType copiedCharacter, @Nullable SkillType copiedSlot, CharacterType character,
                           CoinType coin0, CoinType coin1, CoinType coin2, int nextGunDamage) {
        ClientCombatData.stamina = stamina;
        ClientCombatData.maxStamina = maxStamina;
        ClientCombatData.ultimateGauge = ultimateGauge;
        ClientCombatData.character = character.name();
        coins[0] = coin0;
        coins[1] = coin1;
        coins[2] = coin2;
        ClientCombatData.nextGunDamage = nextGunDamage;

        if (copiedCharacter == CharacterType.NONE || copiedSlot == null) {
            ClientCombatData.copiedSkill = "없음";
            ClientCombatData.copiedSkillId = "none";
        } else {
            ClientCombatData.copiedSkill = skillDisplayName(copiedCharacter, copiedSlot);
            ClientCombatData.copiedSkillId =
                    (copiedCharacter.name() + "_" + copiedSlot.name()).toLowerCase(Locale.ROOT);
        }
    }

    // 복사 스킬의 표시 이름 (예전 CombatSync에서 이동 — 이제 클라에서만 씀)
    private static String skillDisplayName(CharacterType c, SkillType s) {
        if (c == CharacterType.MONKEY && s == SkillType.SKILL_1) {
            return "밀치기";
        }
        if (c == CharacterType.MONKEY && s == SkillType.SKILL_2) {
            return "끌어오기";
        }
        return c.getDisplayName() + (s == SkillType.SKILL_1 ? " 스킬1" : " 스킬2");
    }

    // ----- 레이저 반동용 카운트다운 (LaserStateS2CPacket가 시작, LaserRecoilEvents가 매 틱 감소) -----
    private static int laserChargeLeft = 0;
    private static int laserBeamLeft = 0;

    public static void startLaserState(int chargeTicks, int beamTicks) {
        laserChargeLeft = chargeTicks;
        laserBeamLeft = beamTicks;
    }

    public static void tickLaserState() {
        if (laserChargeLeft > 0) {
            laserChargeLeft--;
        } else if (laserBeamLeft > 0) {
            laserBeamLeft--;
        }
    }

    public static boolean isLaserCharging() { return laserChargeLeft > 0; }
    public static boolean isLaserFiring() { return laserChargeLeft <= 0 && laserBeamLeft > 0; }

    public static int getStamina() { return stamina; }
    public static int getMaxStamina() { return maxStamina; }
    public static int getUltimateGauge() { return ultimateGauge; }
    public static String getCopiedSkill() { return copiedSkill; }
    public static String getCopiedSkillId() { return copiedSkillId; }
    public static String getCharacter() { return character; }
    public static CoinType getCoin(int slot) { return coins[slot]; }
    public static int getNextGunDamage() { return nextGunDamage; }
}
