package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.FearS2CPacket;
import com.example.examplemod.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 모드의 모든 상태 효과를 한 번에 제거 (명령어 /상태초기화 + 사망 시 자동 실행)
// — 스턴, 해커 궁(키반전)·버그무시, 검사 패링·강화, 광전사 격노·광폭화·분노,
//   해머 궁 경직·회전, 도적 표식, 룰렛 연출, 바닐라 물약 효과, 빙결, 공포 표식, 스태미나까지
// (궁극기 게이지는 건드리지 않음 — /removeultimate로 별도 관리)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class StatusCleanser {

    // 죽으면 자동으로 전체 상태 정리
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAll(player);
        }
    }

    public static void clearAll(ServerPlayer player) {
        // ===== 모드 상태 =====
        StunManager.clear(player);                 // 스턴 (해머 투척·궁, 눈덩이, 글리치)
        GuardManager.clear(player);                // 방어 자세
        HammerUltimateEvents.clear(player);        // 해머 궁 시전 경직
        HammerSpinEvents.toggleOff(player);        // 해머 회전 베기 중이면 종료
        SwordsmanEvents.clear(player);             // 검사 패링 표식·강화·연격 예약
        HackerEvents.clear(player);                // 해커 키반전·버그무시·글리치 폭탄
        BerserkerEvents.clear(player);             // 광전사 격노·광폭화·분노 스택
        PhoenixEvents.clear(player);               // 불사조 궁 장판·화상·X 착지 추적
        GamblerEvents.onCasterGone(player);        // 룰렛 연출·돔·무적
        DojukMarkEvents.clearMark(player);         // 도적 표창 표식

        // ===== 자원 =====
        StaminaManager.resetStamina(player, PlayerCharacterManager.getCharacter(player));   // 스태미나 최대치로

        // ===== 바닐라·엔진 상태 =====
        player.removeAllEffects();                 // 물약 효과 전부 (공포 슬로우, 투명 등)
        player.clearFire();                        // 불 끄기
        player.setTicksFrozen(0);                  // 눈덩이 빙결 게이지
        player.setInvulnerable(false);             // 룰렛 등이 남긴 무적

        // 해커: 상태초기화 직후 롤백(Z) 못 쓰게 쿨타임부터 시작
        if (PlayerCharacterManager.getCharacter(player) == CharacterType.HACKER) {
            SkillManager.applyCooldown(player, SkillType.SKILL_1,
                    CharacterStats.getSkillCooldown(CharacterType.HACKER, SkillType.SKILL_1));
        }

        // ===== 클라이언트 표시 리셋 =====
        ModMessages.sendToTrackingAndSelf(new FearS2CPacket(player.getId(), 0), player);   // 머리 위 귀신
        CombatSync.syncStats(player);              // HUD 갱신
    }
}
