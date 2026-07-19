package com.example.examplemod.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class PlayerCombatData {

    // ===== 1. 사물함에 담을 내용물 (필드) =====
    private CharacterType character = CharacterType.NONE;
    private int stamina = 5;
    private int ultimateGauge = 0;

    // ===== 복사 스킬용 (임시값 — NBT 저장/복사 안 함) =====
    private CharacterType copiedCharacter = CharacterType.NONE;  // 복사 보관함: 어느 캐릭터
    private SkillType copiedSlot = null;                          // 복사 보관함: 몇 번 슬롯
    private CharacterType castCharacter = CharacterType.NONE;     // 지금 시전 중인 스킬의 출처
    private SkillType castSlot = null;                            // 지금 시전 중인 슬롯
    private boolean copyBuffActive = false;                       // true면 데미지 +10%

    // ===== 레이저 궁극기용 (임시값) =====
    private int laserChargeTicks = 0;   // 충전(윈드업) 남은 틱
    private int laserTicksLeft = 0;     // 레이저 발사 남은 틱
    private int laserBeamEntityId = -1; // 빔 닻 엔티티 ID (끝나면 제거용)

    public int getLaserBeamEntityId() {
        return laserBeamEntityId;
    }

    public void setLaserBeamEntityId(int id) {
        this.laserBeamEntityId = id;
    }

    // ===== 원숭이 궁극기(여래신장)용 (임시값) =====
    private int buddaWindupTicks = 0;    // 발사 전 충전 남은 틱 (0 = 대기 없음)
    private float buddaDamage = 0.0F;    // 발사할 budda 데미지 (누른 시점 차징으로 계산)

    public int getBuddaWindupTicks() {
        return buddaWindupTicks;
    }

    public void setBuddaWindupTicks(int ticks) {
        this.buddaWindupTicks = ticks;
    }

    public float getBuddaDamage() {
        return buddaDamage;
    }

    public void setBuddaDamage(float damage) {
        this.buddaDamage = damage;
    }

    // ===== 도박사 블랙잭 카드 (임시값) — 카드 붙은 대상, 누적 합, 남은 시간, 머리 위 표시 =====
    private int cardTargetId = -1;    // 카드가 붙은 엔티티 ID (-1 = 없음)
    private int cardSum = 0;          // 누적 합 (21 목표)
    private int cardTicks = 0;        // 카드 남은 틱
    private int cardDisplayId = -1;   // 머리 위 숫자 표시 엔티티 ID

    public int getCardTargetId() {
        return cardTargetId;
    }

    public void setCardTargetId(int id) {
        this.cardTargetId = id;
    }

    public int getCardSum() {
        return cardSum;
    }

    public void setCardSum(int sum) {
        this.cardSum = sum;
    }

    public int getCardTicks() {
        return cardTicks;
    }

    public void setCardTicks(int ticks) {
        this.cardTicks = ticks;
    }

    public int getCardDisplayId() {
        return cardDisplayId;
    }

    public void setCardDisplayId(int id) {
        this.cardDisplayId = id;
    }

    // 카드 맞은 대상 몸에 뜨는 blackjack 모델 표시 엔티티 ID
    private int cardModelDisplayId = -1;

    public int getCardModelDisplayId() {
        return cardModelDisplayId;
    }

    public void setCardModelDisplayId(int id) {
        this.cardModelDisplayId = id;
    }

    // ===== 도박사 룰렛 궁극기 (임시값) — 연출 상태 + 결과 버프/디버프 =====
    private int roulettePartnerId = -1;   // 같이 갇힌 상대 엔티티 ID
    private int rouletteTicks = 0;        // 연출 남은 틱 (0 = 안 함)
    private final CoinType[] rouletteReels = {CoinType.NONE, CoinType.NONE, CoinType.NONE};   // 릴 3개 결과
    private final int[] rouletteDisplayIds = {-1, -1, -1};   // 릴 표시 엔티티 ID
    private final List<BlockPos> rouletteDomeBlocks = new ArrayList<>();   // 돔으로 설치한 블럭 (복원용)
    private int rouletteBuffTicks = 0;         // 7 우세: 내 공격력 +40% 남은 틱
    private int rouletteDebuffTargetId = -1;   // 해골 우세: 디버프 걸린 상대 ID
    private int rouletteDebuffTicks = 0;       // 상대 공격력 -50% 남은 틱

    public int getRoulettePartnerId() {
        return roulettePartnerId;
    }

    public void setRoulettePartnerId(int id) {
        this.roulettePartnerId = id;
    }

    public int getRouletteTicks() {
        return rouletteTicks;
    }

    public void setRouletteTicks(int ticks) {
        this.rouletteTicks = ticks;
    }

    public CoinType getRouletteReel(int i) {
        return rouletteReels[i];
    }

    public void setRouletteReel(int i, CoinType type) {
        rouletteReels[i] = type;
    }

    public int getRouletteDisplayId(int i) {
        return rouletteDisplayIds[i];
    }

    public void setRouletteDisplayId(int i, int id) {
        rouletteDisplayIds[i] = id;
    }

    public List<BlockPos> getRouletteDomeBlocks() {
        return rouletteDomeBlocks;
    }

    // 연출 중 두 참가자를 고정할 좌표 [0-2]=시전자 xyz, [3-5]=상대 xyz
    private final double[] rouletteAnchors = new double[6];

    public double[] getRouletteAnchors() {
        return rouletteAnchors;
    }

    public int getRouletteBuffTicks() {
        return rouletteBuffTicks;
    }

    public void setRouletteBuffTicks(int ticks) {
        this.rouletteBuffTicks = ticks;
    }

    public int getRouletteDebuffTargetId() {
        return rouletteDebuffTargetId;
    }

    public void setRouletteDebuffTargetId(int id) {
        this.rouletteDebuffTargetId = id;
    }

    public int getRouletteDebuffTicks() {
        return rouletteDebuffTicks;
    }

    public void setRouletteDebuffTicks(int ticks) {
        this.rouletteDebuffTicks = ticks;
    }

    // ===== 리볼버 다음 탄환 데미지 (임시값, 미리 굴려서 UI에 표시. 0 = 아직 안 굴림) =====
    private int nextGunDamage = 0;

    public int getNextGunDamage() {
        return nextGunDamage;
    }

    public void setNextGunDamage(int damage) {
        this.nextGunDamage = damage;
    }

    // ===== 도박사 코인용 (임시값) — 슬롯 3개, 각자 종류 + 남은 틱 =====
    private final CoinType[] coinTypes = {CoinType.NONE, CoinType.NONE, CoinType.NONE};
    private final int[] coinTicks = new int[3];

    public CoinType getCoinType(int slot) {
        return coinTypes[slot];
    }

    public int getCoinTicks(int slot) {
        return coinTicks[slot];
    }

    public void setCoin(int slot, CoinType type, int ticks) {
        coinTypes[slot] = type;
        coinTicks[slot] = ticks;
    }

    public void setCoinTicks(int slot, int ticks) {
        coinTicks[slot] = ticks;
    }

    // ===== 도적 X(표창·돌진)용 (임시값) =====
    private int dojukMarkTargetId = -1;   // 표식 대상 엔티티 ID (-1 = 없음)
    private int dojukMarkTicks = 0;       // 표식 남은 틱
    private int dojukMarkDisplayId = -1;  // 대상 머리 위 단검 표시 엔티티 ID
    private int dojukDashTicks = 0;       // 돌진 남은 틱

    public int getDojukMarkTargetId() {
        return dojukMarkTargetId;
    }

    public void setDojukMarkTargetId(int id) {
        this.dojukMarkTargetId = id;
    }

    public int getDojukMarkTicks() {
        return dojukMarkTicks;
    }

    public void setDojukMarkTicks(int ticks) {
        this.dojukMarkTicks = ticks;
    }

    public int getDojukMarkDisplayId() {
        return dojukMarkDisplayId;
    }

    public void setDojukMarkDisplayId(int id) {
        this.dojukMarkDisplayId = id;
    }

    public int getDojukDashTicks() {
        return dojukDashTicks;
    }

    public void setDojukDashTicks(int ticks) {
        this.dojukDashTicks = ticks;
    }

    // ----- 도적 궁극기(투명화) -----
    private int dojukInvisTicks = 0;   // 투명화 남은 틱 (0 = 꺼짐)

    public int getDojukInvisTicks() {
        return dojukInvisTicks;
    }

    public void setDojukInvisTicks(int ticks) {
        this.dojukInvisTicks = ticks;
    }

    // ===== 해머 X(회전 베기)용 (임시값) =====
    private boolean hammerSpinning = false;   // 회전 중 여부
    private int hammerSpinTimer = 0;          // 1초(20틱) 주기 처리용 카운터

    public boolean isHammerSpinning() {
        return hammerSpinning;
    }

    public void setHammerSpinning(boolean spinning) {
        this.hammerSpinning = spinning;
    }

    public int getHammerSpinTimer() {
        return hammerSpinTimer;
    }

    public void setHammerSpinTimer(int timer) {
        this.hammerSpinTimer = timer;
    }

    // ===== 스킬 쿨타임용 (임시값) — 슬롯별 "다시 가능해지는 게임시각(틱)" =====
    private long skill1ReadyTick = 0L;
    private long skill2ReadyTick = 0L;
    private long ultimateReadyTick = 0L;

    public long getSkillReadyTick(SkillType slot) {
        return switch (slot) {
            case SKILL_1 -> skill1ReadyTick;
            case SKILL_2 -> skill2ReadyTick;
            case ULTIMATE -> ultimateReadyTick;
            default -> 0L;
        };
    }

    public void setSkillReadyTick(SkillType slot, long tick) {
        switch (slot) {
            case SKILL_1 -> skill1ReadyTick = tick;
            case SKILL_2 -> skill2ReadyTick = tick;
            case ULTIMATE -> ultimateReadyTick = tick;
            default -> {
            }
        }
    }

    // ===== 2. 꺼내고 넣는 통로 (게터/세터) =====
    // 대쉬 쿨타임용: 이 게임시각(틱) 이후에 다시 대쉬 가능. NBT 저장 안 함(잠깐 값).
    private long dashReadyTick = 0L;

    public long getDashReadyTick() {
        return dashReadyTick;
    }

    public void setDashReadyTick(long tick) {
        this.dashReadyTick = tick;
    }

    // 스태미나 재생 타이머 (임시값 — 예전 static HashMap 대체)
    private int staminaRegenTimer = 0;

    public int getStaminaRegenTimer() {
        return staminaRegenTimer;
    }

    public void setStaminaRegenTimer(int timer) {
        this.staminaRegenTimer = timer;
    }

    
    public CharacterType getCharacter() {
        return character;
    }

    public void setCharacter(CharacterType character) {
        this.character = character;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getUltimateGauge() {
        return ultimateGauge;
    }

    public void setUltimateGauge(int ultimateGauge) {
        this.ultimateGauge = ultimateGauge;
    }

    // ----- 복사 스킬 보관함 -----
    public CharacterType getCopiedCharacter() {
        return copiedCharacter;
    }

    public void setCopiedCharacter(CharacterType copiedCharacter) {
        this.copiedCharacter = copiedCharacter;
    }

    public SkillType getCopiedSlot() {
        return copiedSlot;
    }

    public void setCopiedSlot(SkillType copiedSlot) {
        this.copiedSlot = copiedSlot;
    }

    // ----- 시전 중인 스킬 출처 (투사체 도장용) -----
    public CharacterType getCastCharacter() {
        return castCharacter;
    }

    public void setCastCharacter(CharacterType castCharacter) {
        this.castCharacter = castCharacter;
    }

    public SkillType getCastSlot() {
        return castSlot;
    }

    public void setCastSlot(SkillType castSlot) {
        this.castSlot = castSlot;
    }

    // ----- 데미지 +10% 버프 -----
    public boolean isCopyBuffActive() {
        return copyBuffActive;
    }

    public void setCopyBuffActive(boolean copyBuffActive) {
        this.copyBuffActive = copyBuffActive;
    }

    // ----- 레이저 궁극기 -----
    public int getLaserChargeTicks() {
        return laserChargeTicks;
    }

    public void setLaserChargeTicks(int laserChargeTicks) {
        this.laserChargeTicks = laserChargeTicks;
    }

    public int getLaserTicksLeft() {
        return laserTicksLeft;
    }

    public void setLaserTicksLeft(int laserTicksLeft) {
        this.laserTicksLeft = laserTicksLeft;
    }

    // ===== 3. 헌 사물함 → 새 사물함 복사 (단계 4 부활용) =====
    public void copyFrom(PlayerCombatData source) {
        this.character = source.character;
        this.stamina = source.stamina;
        this.ultimateGauge = source.ultimateGauge;
    }

    // ===== 4. 사물함 내용물 → NBT (저장할 때) =====
    public void saveNBTData(CompoundTag nbt) {
        nbt.putString("character", character.name());
        nbt.putInt("stamina", stamina);
        nbt.putInt("ultimateGauge", ultimateGauge);
    }

    // ===== 5. NBT → 사물함 내용물 (불러올 때) =====
    public void loadNBTData(CompoundTag nbt) {
        // 잘못된 문자열이 들어와도 게임이 안 터지게 방어
        try {
            this.character = CharacterType.valueOf(nbt.getString("character"));
        } catch (IllegalArgumentException e) {
            this.character = CharacterType.NONE;
        }

        this.stamina = nbt.getInt("stamina");
        this.ultimateGauge = nbt.getInt("ultimateGauge");
    }
}