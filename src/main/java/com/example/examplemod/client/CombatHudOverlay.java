package com.example.examplemod.client;

import com.example.examplemod.item.RevolverItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.Map;

public class CombatHudOverlay {

    // 텍스처 경로는 한 번만 생성 (매 프레임 new 하면 GC 부담)
    private static final ResourceLocation ICON_STAMINA = new ResourceLocation("examplemod", "textures/gui/hud_stamina.png");
    private static final ResourceLocation ICON_ULT = new ResourceLocation("examplemod", "textures/gui/hud_ult.png");
    private static final ResourceLocation GUARD_SHIELD = new ResourceLocation("examplemod", "textures/gui/guard_shield.png");
    private static final ResourceLocation GUARD_SHIELD_HIT = new ResourceLocation("examplemod", "textures/gui/guard_shield_hit.png");
    private static final ResourceLocation SKILL_BOX = new ResourceLocation("examplemod", "textures/gui/skill_box.png");
    private static final ResourceLocation CHARGE_BAR_BG = new ResourceLocation("examplemod", "textures/gui/charge_bar_bg.png");
    private static final ResourceLocation CHARGE_BAR_FULL = new ResourceLocation("examplemod", "textures/gui/charge_bar_full.png");
    // 도박사 코인 (32x32 PNG — 교체 시 같은 크기로)
    private static final ResourceLocation COIN_DEFAULT = new ResourceLocation("examplemod", "textures/gui/coin_default.png");
    private static final ResourceLocation COIN_SEVEN = new ResourceLocation("examplemod", "textures/gui/coin_seven.png");
    private static final ResourceLocation COIN_SKULL = new ResourceLocation("examplemod", "textures/gui/coin_skull.png");
    private static final ResourceLocation PARRY_MARK_HUD = new ResourceLocation("examplemod", "textures/gui/samurai_x.png");
    private static final ResourceLocation RAGE_HUD = new ResourceLocation("examplemod", "textures/gui/rage.png");
    // 스킬 프로필 경로 캐시 (skillId별 한 번만 생성)
    private static final Map<String, ResourceLocation> PROFILE_CACHE = new HashMap<>();
    // 프로필 PNG 존재 여부 캐시 — 매 프레임 파일 조회 방지 (PNG를 새로 넣으면 게임 재시작 필요)
    private static final Map<String, Boolean> PROFILE_EXISTS = new HashMap<>();
    // 스킬 쿨타임 HUD 아이콘 캐시 (캐릭터_슬롯별, 예: hammer_z)
    private static final Map<String, ResourceLocation> ICON_CACHE = new HashMap<>();
    private static final Map<String, Boolean> ICON_EXISTS = new HashMap<>();

    public static final IGuiOverlay HUD =
            (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) -> {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // 채팅은 자기 UI를 Z+100 위에 그린다. 그보다 높은 Z로 올려야 채팅 검은 UI에 안 가려짐.
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 250);

        // 이미지(blit) 그리기 전 셰이더 색을 흰색 불투명으로 (모든 바·박스 공통)
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // ===== 왼쪽 아래: 스태미나(칸 분할) / 궁 게이지 바. HP는 바닐라 하트로 표시 =====
        {
            int iconSize = 12;
            int barX = 24, barH = 12;   // 왼쪽 12px는 아이콘 자리
            int rowGap = barH + 3;
            // 바 폭: 기본 119, 단 핫바 왼쪽 끝 6칸 앞까지만 — 노트북처럼 GUI 배율 적용 후 화면 폭이 좁으면(470 미만)
            // 고정 폭이 핫바 첫 칸·하트를 덮었음. 그래도 60칸이 안 나오는 아주 좁은 창이면 하트·방어 줄 위로 올림
            int barW = Math.min(119, width / 2 - 91 - 6 - barX);
            int stY = height - 10 - barH - rowGap;   // 스태미나 위, 게이지 아래
            if (barW < 60) {
                barW = 119;
                stY = vanillaLeftStackTop(mc, height) - 3 - barH - rowGap;
            }
            int ultY = stY + rowGap;

            // --- 스태미나: 최대치만큼 칸으로 분할 (대쉬 1 = 눈에 보이는 1칸) ---
            int st = ClientCombatData.getStamina();
            int stMax = Math.max(1, ClientCombatData.getMaxStamina());
            int sx = barX + HudFx.shakeOffset(HudFx.staminaShakeTicks);

            guiGraphics.blit(ICON_STAMINA, sx - 15, stY, iconSize, iconSize, 0, 0, 12, 12, 12, 12);

            // 배경 판 (부족 흔들림 중엔 빨간 테두리)
            int stFrame = HudFx.staminaShakeTicks > 0 ? 0xD0C03030 : 0xA0000000;
            guiGraphics.fill(sx - 1, stY - 1, sx + barW + 1, stY + barH + 1, stFrame);

            int cellGap = 2;
            float cellW = (barW - (stMax - 1) * cellGap) / (float) stMax;
            for (int i = 0; i < stMax; i++) {
                int cx0 = sx + Math.round(i * (cellW + cellGap));
                int cx1 = Math.min(sx + barW, cx0 + Math.max(1, Math.round(cellW)));
                if (i < st) {
                    // 채워진 칸: 초록 그라데이션 + 상단 하이라이트
                    guiGraphics.fillGradient(cx0, stY, cx1, stY + barH, 0xFF8FE060, 0xFF3E8C2A);
                    guiGraphics.fill(cx0, stY, cx1, stY + 2, 0x60FFFFFF);
                } else {
                    guiGraphics.fill(cx0, stY, cx1, stY + barH, 0xFF14181C);
                }
                // 소모 잔상: 방금 줄어든 칸이 하얗게 번쩍였다 사라짐
                if (HudFx.consumeFlashTicks > 0 && i >= HudFx.consumeTo && i < HudFx.consumeFrom) {
                    int a = 0xC0 * HudFx.consumeFlashTicks / HudFx.CONSUME_FLASH_MAX;
                    guiGraphics.fill(cx0, stY, cx1, stY + barH, (a << 24) | 0xFFFFFF);
                }
                // 회복 반짝: 방금 찬 칸
                if (HudFx.gainFlashTicks > 0 && i == HudFx.gainCell) {
                    int a = 0x90 * HudFx.gainFlashTicks / HudFx.GAIN_FLASH_MAX;
                    guiGraphics.fill(cx0, stY, cx1, stY + barH, (a << 24) | 0xFFFFFF);
                }
            }

            // --- 궁 게이지: 그라데이션 + 완충 시 금색 발광 펄스 ---
            int gauge = ClientCombatData.getUltimateGauge();
            boolean ready = gauge >= 100;
            int ux = barX + HudFx.shakeOffset(HudFx.ultShakeTicks);

            guiGraphics.blit(ICON_ULT, ux - 15, ultY, iconSize, iconSize, 0, 0, 12, 12, 12, 12);

            long time = mc.level != null ? mc.level.getGameTime() : 0L;
            float pulse = (float) (Math.sin((time + partialTick) * 0.35) + 1.0) / 2.0F;

            int ultFrame = HudFx.ultShakeTicks > 0 ? 0xD0C03030
                    : ready ? (((0x70 + (int) (pulse * 0x8F)) << 24) | 0xFFC940) : 0xA0000000;
            guiGraphics.fill(ux - 1, ultY - 1, ux + barW + 1, ultY + barH + 1, ultFrame);
            guiGraphics.fill(ux, ultY, ux + barW, ultY + barH, 0xFF14181C);

            int fillW = Math.round(barW * Math.min(100, gauge) / 100.0F);
            if (fillW > 0) {
                if (ready) {
                    guiGraphics.fillGradient(ux, ultY, ux + fillW, ultY + barH, 0xFFFFD75A, 0xFFB07E14);
                    guiGraphics.fill(ux, ultY, ux + fillW, ultY + barH, ((int) (pulse * 0x50) << 24) | 0xFFFFFF);
                } else {
                    guiGraphics.fillGradient(ux, ultY, ux + fillW, ultY + barH, 0xFFB07AFF, 0xFF5A2CA0);
                }
                guiGraphics.fill(ux, ultY, ux + fillW, ultY + 2, 0x50FFFFFF);
            }
            // 완충 순간 화이트 플래시 (+ "띠링"은 HudFx가 재생)
            if (HudFx.ultFlashTicks > 0) {
                int a = 0xB0 * HudFx.ultFlashTicks / HudFx.ULT_FLASH_MAX;
                guiGraphics.fill(ux, ultY, ux + barW, ultY + barH, (a << 24) | 0xFFFFFF);
            }

            int textDY = (barH - 8) / 2;
            if (ready) {
                guiGraphics.drawCenteredString(mc.font, "준비 완료!", ux + barW / 2, ultY + textDY, 0xFFF0B0);
            } else {
                guiGraphics.drawCenteredString(mc.font, gauge + " / 100", ux + barW / 2, ultY + textDY, 0xFFFFFF);
            }
        }

        // ===== 사이보그 전용 복사 스킬 박스 (배고픔바 자리) — 사이보그일 때만 표시 =====
        if ("CYBORG".equals(ClientCombatData.getCharacter())) {
            int boxW = 80, boxH = 20;
            int boxX = width / 2 + 10;   // 핫바 오른쪽
            int boxY = height - 50;      // 위치 조절

            // 박스 배경 (텍스처 — 나중에 이 png만 교체하면 됨)
            guiGraphics.blit(SKILL_BOX, boxX, boxY, boxW, boxH, 0, 0, boxW, boxH, boxW, boxH);

            // 왼쪽 프로필 이미지 (복사 스킬 있고 + PNG가 실제로 있을 때만)
            String skillId = ClientCombatData.getCopiedSkillId();
            if (!"none".equals(skillId)) {
                ResourceLocation profile = PROFILE_CACHE.computeIfAbsent(skillId,
                        id -> new ResourceLocation("examplemod", "textures/gui/skill/" + id + ".png"));
                boolean exists = PROFILE_EXISTS.computeIfAbsent(skillId,
                        id -> mc.getResourceManager().getResource(profile).isPresent());
                if (exists) {
                    guiGraphics.blit(profile, boxX + 2, boxY + 2, 16, 16, 0, 0, 32, 32, 32, 32);
                }
            }

            // 오른쪽 스킬 이름
            guiGraphics.drawString(mc.font, ClientCombatData.getCopiedSkill(), boxX + 22, boxY + 6, 0xFFFFFF);
        }

        // ===== 도박사 전용 코인 3개 (배고픔바 자리) — 도박사일 때만 표시 =====
        if ("GAMBLER".equals(ClientCombatData.getCharacter())) {
            int coinSize = 24;            // 코인 표시 크기 (px) — 여기만 바꾸면 됨
            int coinGap = 2;              // 코인 사이 간격
            int coinX = width / 2 + 4;    // 핫바 오른쪽 (체력 오른편) — 살짝 왼쪽으로
            int coinY = height - 50;      // 살짝 아래로
            for (int i = 0; i < 3; i++) {
                ResourceLocation tex = switch (ClientCombatData.getCoin(i)) {
                    case SEVEN -> COIN_SEVEN;
                    case SKULL -> COIN_SKULL;
                    default -> COIN_DEFAULT;
                };
                guiGraphics.blit(tex, coinX + i * (coinSize + coinGap), coinY,
                        coinSize, coinSize, 0, 0, 32, 32, 32, 32);
            }
        }

        // ===== 검사 전용: 남은 패링 표식 (코인과 같은 자리) — 표식이 있을 때만 표시 =====
        if ("SWORDSMAN".equals(ClientCombatData.getCharacter())) {
            int marks = ClientParryMarks.get(mc.player.getId());
            if (marks > 0) {
                int markSize = 16;            // 표식 표시 크기 (px)
                int markGap = 2;              // 표식 사이 간격
                int markX = width / 2 + 4;    // 핫바 오른쪽 (코인 자리와 동일)
                int markY = height - 46;      // 코인 자리보다 4px 아래
                for (int i = 0; i < marks; i++) {
                    guiGraphics.blit(PARRY_MARK_HUD, markX + i * (markSize + markGap), markY,
                            markSize, markSize, 0, 0, 32, 32, 32, 32);
                }
            }
        }

        // ===== 광전사 전용: 분노 스택 (핫바 위, 패링 표식과 같은 자리) =====
        if ("BERSERKER".equals(ClientCombatData.getCharacter())) {
            int rage = ClientRageData.get();
            if (rage > 0) {
                int rageSize = 16;            // 분노 아이콘 크기 (px)
                int rageGap = 2;              // 아이콘 사이 간격
                int rageX = width / 2 + 4;    // 핫바 오른쪽
                int rageY = height - 46;
                for (int i = 0; i < rage; i++) {
                    guiGraphics.blit(RAGE_HUD, rageX + i * (rageSize + rageGap), rageY,
                            rageSize, rageSize, 0, 0, 32, 32, 32, 32);
                }
            }
        }

        // ===== 오른쪽 위: 스킬 쿨타임 HUD (Z / X / 궁 아이콘) =====
        String hudChar = ClientCombatData.getCharacter();
        if (hudChar != null && !hudChar.isEmpty() && !"NONE".equals(hudChar)) {
            String charLower = hudChar.toLowerCase(java.util.Locale.ROOT);
            String[] slotKeys = {"z", "x", "ult"};
            String[] slotLabels = {"Z", "X", "궁"};
            com.example.examplemod.combat.SkillType[] slotTypes = {
                    com.example.examplemod.combat.SkillType.SKILL_1,
                    com.example.examplemod.combat.SkillType.SKILL_2,
                    com.example.examplemod.combat.SkillType.ULTIMATE};
            int iconSize = 20, iconGap = 4;
            int rowX = 8;    // 왼쪽 위 (오른쪽 위는 물약 효과 아이콘 자리)
            int rowY = 8;

            for (int i = 0; i < 3; i++) {
                int x = rowX + i * (iconSize + iconGap);
                String iconId = charLower + "_" + slotKeys[i];
                ResourceLocation icon = ICON_CACHE.computeIfAbsent(iconId,
                        k -> new ResourceLocation("examplemod", "textures/gui/skill/" + k + ".png"));
                boolean iconExists = ICON_EXISTS.computeIfAbsent(iconId,
                        k -> mc.getResourceManager().getResource(icon).isPresent());

                // 배경 판
                guiGraphics.fill(x - 1, rowY - 1, x + iconSize + 1, rowY + iconSize + 1, 0x90000000);
                if (iconExists) {
                    guiGraphics.blit(icon, x, rowY, iconSize, iconSize, 0, 0, 128, 128, 128, 128);
                }

                if (slotTypes[i] == com.example.examplemod.combat.SkillType.ULTIMATE) {
                    // 궁: 게이지가 다 찰 때까지 어둡게
                    if (ClientCombatData.getUltimateGauge() < 100) {
                        guiGraphics.fill(x, rowY, x + iconSize, rowY + iconSize, 0xA0101010);
                    }
                } else {
                    int remain = ClientSkillCooldowns.getRemaining(slotTypes[i]);
                    if (remain > 0) {
                        // 쿨타임: 위에서부터 걷히는 어두운 덮개 + 남은 초
                        int total = Math.max(1, ClientSkillCooldowns.getTotal(slotTypes[i]));
                        int coverH = (int) Math.ceil(iconSize * (remain / (float) total));
                        guiGraphics.fill(x, rowY, x + iconSize, rowY + coverH, 0xB0101010);
                        String sec = String.valueOf((int) Math.ceil(remain / 20.0));
                        guiGraphics.drawCenteredString(mc.font, sec, x + iconSize / 2, rowY + 6, 0xFFD700);
                    }
                }

                // 키 라벨
                guiGraphics.drawCenteredString(mc.font, slotLabels[i], x + iconSize / 2, rowY + iconSize + 3, 0xB0B0B0);
            }
        }

        // ===== 오른쪽 위: 다음 탄환 데미지 (리볼버를 들었을 때만) — 바닐라 효과 아이콘 두 줄(좋은 효과 y1~25, 해로운 효과 y27~51) 아래 =====
        if (mc.player.getMainHandItem().getItem() instanceof RevolverItem) {
            String txt = "다음 탄환: " + ClientCombatData.getNextGunDamage();
            int tw = mc.font.width(txt);
            guiGraphics.drawString(mc.font, txt, width - tw - 10, 56, 0xFFD700);
        }

        // ===== 히트마커: 적중 시 조준점 주위 X자 4획 (처치 시 빨갛고 크게) =====
        {
            float hm = ClientHitMarker.progress(partialTick);
            if (hm > 0f) {
                boolean kill = ClientHitMarker.isKill();
                int cx = width / 2, cy = height / 2;
                float expand = (1f - hm) * 2f;                       // 시간이 지나며 살짝 바깥으로
                float inner = (kill ? 5f : 4f) + expand;             // 획 안쪽 시작 거리
                float len = kill ? 5f : 4f;                          // 획 길이
                int alpha = (int) (0xE0 * hm);
                int color = (alpha << 24) | (kill ? 0xFF3A3A : 0xFFFFFF);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(cx, cy, 0);
                guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45));
                int in = Math.round(inner), out = Math.round(inner + len);
                guiGraphics.fill(0, -out, 1, -in, color);            // 위 (1px 획)
                guiGraphics.fill(0, in, 1, out, color);              // 아래
                guiGraphics.fill(-out, 0, -in, 1, color);            // 왼쪽
                guiGraphics.fill(in, 0, out, 1, color);              // 오른쪽
                guiGraphics.pose().popPose();
            }
        }

        // ===== 가드 중: 크로스헤어 아래 방패 아이콘 (막아내면 커졌다 돌아오는 펄스) =====
        if (ClientSkillKeyEvents.isGuardingNow()) {
            float pulse = ClientGuardFx.iconPulse(partialTick);
            int size = 14 + Math.round(pulse * 6);       // 평소 14px, 막아낸 직후 최대 20px
            int gx = width / 2 - size / 2;
            int gy = height / 2 + 12 - (size - 14) / 2;  // 커질 때 중심 유지
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.85f + pulse * 0.15f);
            // 막아낸 직후(펄스 중)엔 별 박힌 방패로 교체 (32x32 원본 이미지)
            guiGraphics.blit(pulse > 0f ? GUARD_SHIELD_HIT : GUARD_SHIELD,
                    gx, gy, size, size, 0, 0, 32, 32, 32, 32);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        // 차징 진행 바 (경험치바 자리, 노란색) — 차징 중에만 표시
        int chargeTick = ClientSkillKeyEvents.getActiveChargeTick();
        if (chargeTick > 0) {
            int max = ClientSkillKeyEvents.getMaxChargeTick();
            int barW = 182, barH = 6;
            int barX = width / 2 - barW / 2;   // 핫바 폭, 화면 중앙
            int barY = height - 30;            // 경험치바 자리 (핫바 바로 위)

            guiGraphics.blit(CHARGE_BAR_BG, barX, barY, barW, barH, 0, 0, barW, barH, barW, barH);
            int fillW = barW * Math.min(chargeTick, max) / max;   // 차징한 만큼 왼쪽부터 채움
            if (fillW > 0) {
                guiGraphics.blit(CHARGE_BAR_FULL, barX, barY, fillW, barH, 0, 0, barW, barH, barW, barH);
            }
        }

        guiGraphics.pose().popPose();
    };

    // 바닐라 왼쪽 더미(하트 여러 줄 + 방어 줄)의 가장 위 y — Gui.renderPlayerHealth와 같은 계산 (흡수 하트·최대 체력 반영)
    private static int vanillaLeftStackTop(Minecraft mc, int height) {
        float maxHp = Math.max((float) mc.player.getAttributeValue(Attributes.MAX_HEALTH), mc.player.getHealth());
        int absorb = Mth.ceil(mc.player.getAbsorptionAmount());
        int rows = Mth.ceil((maxHp + absorb) / 2.0F / 10.0F);
        int rowH = Math.max(10 - (rows - 2), 3);
        return height - 39 - (rows - 1) * rowH - 10;
    }

}
