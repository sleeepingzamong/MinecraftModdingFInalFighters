package com.example.examplemod.client;

import com.example.examplemod.item.RevolverItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.Map;

public class CombatHudOverlay {

    // 텍스처 경로는 한 번만 생성 (매 프레임 new 하면 GC 부담)
    private static final ResourceLocation BAR_BG = new ResourceLocation("examplemod", "textures/gui/bar_bg.png");
    private static final ResourceLocation STAMINA_FILL = new ResourceLocation("examplemod", "textures/gui/stamina_fill.png");
    private static final ResourceLocation GAUGE_FILL = new ResourceLocation("examplemod", "textures/gui/gauge_fill.png");
    private static final ResourceLocation SKILL_BOX = new ResourceLocation("examplemod", "textures/gui/skill_box.png");
    private static final ResourceLocation CHARGE_BAR_BG = new ResourceLocation("examplemod", "textures/gui/charge_bar_bg.png");
    private static final ResourceLocation CHARGE_BAR_FULL = new ResourceLocation("examplemod", "textures/gui/charge_bar_full.png");
    // 도박사 코인 (32x32 PNG — 교체 시 같은 크기로)
    private static final ResourceLocation COIN_DEFAULT = new ResourceLocation("examplemod", "textures/gui/coin_default.png");
    private static final ResourceLocation COIN_SEVEN = new ResourceLocation("examplemod", "textures/gui/coin_seven.png");
    private static final ResourceLocation COIN_SKULL = new ResourceLocation("examplemod", "textures/gui/coin_skull.png");
    private static final ResourceLocation PARRY_MARK_HUD = new ResourceLocation("examplemod", "textures/gui/samurai_x.png");
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

        // ===== 왼쪽 아래: 스태미나 / 게이지 바 (글자는 바 가운데). HP는 바닐라 하트로 표시 =====
        {
            int barX = 10, barW = 133, barH = 12;
            int rowGap = barH + 3;
            int row = height - 10 - barH - rowGap;   // 맨 아래에서 위로 쌓기 (스태미나 위, 게이지 아래)
            int cx = barX + barW / 2;          // 바 가로 중앙
            int textDY = (barH - 8) / 2;       // 8px 글자를 바 세로 중앙에

            int st = ClientCombatData.getStamina();
            int stMax = ClientCombatData.getMaxStamina();
            drawBar(guiGraphics, barX, row, barW, barH, stMax > 0 ? (float) st / stMax : 0,
                    BAR_BG, STAMINA_FILL);
            guiGraphics.drawCenteredString(mc.font, "ST " + st + "/" + stMax, cx, row + textDY, 0xFFFFFF);

            int gauge = ClientCombatData.getUltimateGauge();
            drawBar(guiGraphics, barX, row + rowGap, barW, barH, gauge / 100.0F,
                    BAR_BG, GAUGE_FILL);
            guiGraphics.drawCenteredString(mc.font, "궁 " + gauge + "/100", cx, row + rowGap + textDY, 0xFFFFFF);
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
            int rowX = width - 3 * iconSize - 2 * iconGap - 8;
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
                    guiGraphics.blit(icon, x, rowY, iconSize, iconSize, 0, 0, 32, 32, 32, 32);
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

        // ===== 오른쪽 위: 다음 탄환 데미지 (리볼버를 들었을 때만) — 스킬 아이콘 줄 아래 =====
        if (mc.player.getMainHandItem().getItem() instanceof RevolverItem) {
            String txt = "다음 탄환: " + ClientCombatData.getNextGunDamage();
            int tw = mc.font.width(txt);
            guiGraphics.drawString(mc.font, txt, width - tw - 10, 42, 0xFFD700);
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

    // 바 하나 그리기: 배경 → 채움(왼쪽부터 ratio만큼). ratio 0~1.
    private static void drawBar(GuiGraphics g, int x, int y, int w, int h, float ratio,
                                ResourceLocation bg, ResourceLocation fill) {
        g.blit(bg, x, y, w, h, 0, 0, w, h, w, h);
        int fillW = Math.round(w * Math.max(0.0F, Math.min(1.0F, ratio)));
        if (fillW > 0) {
            g.blit(fill, x, y, fillW, h, 0, 0, w, h, w, h);
        }
    }
}
