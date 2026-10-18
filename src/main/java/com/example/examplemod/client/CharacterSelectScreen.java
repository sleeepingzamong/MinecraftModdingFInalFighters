package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.combat.CharacterStats;
import com.example.examplemod.combat.CharacterType;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.SelectCharacterC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 회전 휠 캐릭터 선택창.
 * - 화면 크기에 맞춰 UI 전체가 자동 확대 (논리 좌표 300×210 기준, 최대 2.5배)
 * - 회전 중에는 부채꼴 판·선택 하이라이트·캐릭터가 전부 한 덩어리로 같이 돎.
 *   판이 SLOTS조각 대칭이라 SLOT_ANGLE마다 원위치와 같아짐 → 멈추면 원본 텍스처를 그대로 그려 항상 선명.
 * - 캐릭터 추가 시: CharacterType에 항목 추가 + GenWheel9(스크래치 생성기)의 N을 맞춰 휠 텍스처 재생성.
 * - 조작: ←/→(A/D) 회전, Enter/가운데 원 클릭 = 확정, 부채꼴 클릭 = 그 캐릭터를 12시로 회전
 * - 초상화(portrait_이름.png)가 폴더에 있으면 자동 표시, 없으면 이름 글자만
 */
public class CharacterSelectScreen extends Screen {

    private static final ResourceLocation BG =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/select/select_bg.png");
    private static final ResourceLocation WHEEL =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/select/wheel.png");
    private static final ResourceLocation WHEEL_SELECTED =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/select/wheel_selected.png");
    // 휠 칸 수 = 캐릭터 수 (NONE 제외). 휠 텍스처(wheel*.png)도 같은 조각 수로 구워져 있어야 함.
    private static final int SLOTS = CharacterType.values().length - 1;
    private static final CharacterType[] WHEEL_ORDER = {
            CharacterType.MONKEY, CharacterType.CYBORG, CharacterType.GAMBLER, CharacterType.DOJUK, CharacterType.SWORDSMAN,
            CharacterType.PHOENIX, CharacterType.HAMMER, CharacterType.BERSERKER, CharacterType.HACKER};
    private static final float SLOT_ANGLE = 360f / SLOTS;

    private static final ResourceLocation[] WHEEL_HOVER = new ResourceLocation[SLOTS];
    private static final ResourceLocation CENTER =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/select/center_circle.png");

    static {
        for (int k = 0; k < SLOTS; k++) {
            WHEEL_HOVER[k] = new ResourceLocation(ExampleMod.MODID,
                    "textures/gui/select/wheel_hover_" + k + ".png");
        }
    }

    // 논리 좌표계 (이 크기로 그려놓고 통째로 확대)
    private static final int LOGICAL_W = 380, LOGICAL_H = 210;
    private static final int WHEEL_CX = 110;   // 휠 중심 X (오른쪽은 설명란)
    private static final int INFO_X = 212;     // 설명란 시작 X (배경 구분선 204 오른쪽)
    private static final int WHEEL_SIZE = 176;
    private static final double R_IN = 24, R_OUT = 86;   // 부채꼴 안/바깥 반지름 (텍스처와 일치)
    private static final int PORTRAIT_R = 52;            // 초상화 중심 반지름
    private static final int NAME_R = 74;                // 초상화가 있을 때 이름 반지름
    private static final float MAX_SCALE = 2.5f;

    private static final int COL_NAME_IDLE = 0xCFC5BC;
    private static final int COL_NAME_SEL = 0xFFFFFF;
    private static final int COL_HINT = 0x8A8078;
    private static final int COL_POINTER = 0xFFF0B462;

    private final CharacterType[] chars;         // NONE 제외 — 8칸 휠 기준
    private final ResourceLocation[] portraits;
    private final boolean[] hasPortrait;

    private int selectedIndex = 0;
    private float rot = 0f;        // 현재 회전 각도
    private float targetRot = 0f;  // 목표 회전 각도 (누적값)

    // 제한시간: 20초 안에 안 고르면 무작위 캐릭터 자동 확정
    private static final int AUTO_PICK_TICKS = 20 * 20;
    private int autoPickLeft = AUTO_PICK_TICKS;

    // 화면 → 논리 좌표 변환용 (updateLayout에서 갱신)
    private float uiScale = 1f;
    private float originX = 0f;
    private float originY = 0f;

    public CharacterSelectScreen() {
        super(Component.literal("캐릭터 선택"));

        // 휠 순서 (12시부터 시계방향) — 선택창에서만 쓰는 순서라 CharacterType 선언 순서와 별개.
        // 여기 없는 캐릭터(새로 추가된 캐릭터)는 뒤에 자동으로 붙음
        List<CharacterType> list = new ArrayList<>(List.of(WHEEL_ORDER));
        for (CharacterType type : CharacterType.values()) {
            if (type != CharacterType.NONE && !list.contains(type)) {
                list.add(type);
            }
        }
        chars = list.toArray(new CharacterType[0]);

        portraits = new ResourceLocation[chars.length];
        hasPortrait = new boolean[chars.length];
        for (int i = 0; i < chars.length; i++) {
            portraits[i] = new ResourceLocation(ExampleMod.MODID,
                    "textures/gui/select/portrait_" + chars[i].name().toLowerCase(Locale.ROOT) + ".png");
            hasPortrait[i] = Minecraft.getInstance().getResourceManager()
                    .getResource(portraits[i]).isPresent();
        }
    }

    // 화면 크기에 맞춰 확대 배율과 그리기 시작점 계산
    private void updateLayout() {
        uiScale = Math.min(MAX_SCALE, Math.min(
                (this.width - 8) / (float) LOGICAL_W,
                (this.height - 8) / (float) LOGICAL_H));
        uiScale = Math.max(1f, uiScale);
        originX = (this.width - LOGICAL_W * uiScale) / 2f;
        originY = (this.height - LOGICAL_H * uiScale) / 2f;
    }

    private double toLogicalX(double screenX) { return (screenX - originX) / uiScale; }
    private double toLogicalY(double screenY) { return (screenY - originY) / uiScale; }

    private boolean atRest() {
        return rot == targetRot;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        updateLayout();

        // 회전 보간: 매 프레임 목표 각도로 부드럽게 접근
        rot += (targetRot - rot) * 0.25f;
        if (Math.abs(targetRot - rot) < 0.1f) {
            rot = targetRot;
        }

        double lmx = toLogicalX(mouseX), lmy = toLogicalY(mouseY);
        int cx = WHEEL_CX, cy = LOGICAL_H / 2;
        int wheelX = cx - WHEEL_SIZE / 2, wheelY = cy - WHEEL_SIZE / 2;

        RenderSystem.enableBlend();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(originX, originY, 0);
        guiGraphics.pose().scale(uiScale, uiScale, 1f);

        guiGraphics.blit(BG, 0, 0, 0, 0, LOGICAL_W, LOGICAL_H, LOGICAL_W, LOGICAL_H);

        // 휠 판: 회전 중에는 판도 같이 돎. SLOTS조각 대칭이라 반 칸 범위만 돌려도
        // 시각적으로 연속 회전과 동일 → 멈추면 spin=0이 되어 원본 그대로 (항상 선명)
        float spin = rot - SLOT_ANGLE * Math.round(rot / SLOT_ANGLE);
        if (Math.abs(spin) < 0.05f) {
            guiGraphics.blit(WHEEL, wheelX, wheelY, 0, 0, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE);
        } else {
            drawWheelRotated(guiGraphics, WHEEL, spin, cx, cy);
        }

        // 선택 하이라이트: 선택된 캐릭터의 조각에 붙어서 함께 12시로 미끄러져 들어옴
        float selAngle = selectedIndex * SLOT_ANGLE + rot;
        selAngle -= 360f * Math.round(selAngle / 360f);
        if (Math.abs(selAngle) < 0.05f) {
            guiGraphics.blit(WHEEL_SELECTED, wheelX, wheelY, 0, 0, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE);
        } else {
            drawWheelRotated(guiGraphics, WHEEL_SELECTED, selAngle, cx, cy);
        }

        // 호버: 멈춰 있을 때만, 방향별로 미리 구운 조각이라 항상 선명 (12시는 선택색이라 제외)
        if (atRest()) {
            int hoverSlot = slotAt(lmx, lmy, cx, cy);
            if (hoverSlot > 0) {
                guiGraphics.blit(WHEEL_HOVER[hoverSlot], wheelX, wheelY,
                        0, 0, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE);
            }
        }

        // 제목 + 12시 포인터 삼각형
        guiGraphics.drawCenteredString(this.font, this.title, cx, 4, COL_NAME_SEL);
        for (int i = 0; i < 5; i++) {
            int half = 5 - i;
            guiGraphics.fill(cx - half, wheelY - 5 + i, cx + half, wheelY - 4 + i, COL_POINTER);
        }

        // 캐릭터(초상화+이름): 궤도를 따라 돌되 기울지는 않음 (관람차 방식)
        for (int i = 0; i < chars.length; i++) {
            double angle = Math.toRadians(i * SLOT_ANGLE + rot);
            int px = (int) Math.round(cx + PORTRAIT_R * Math.sin(angle));
            int py = (int) Math.round(cy - PORTRAIT_R * Math.cos(angle));
            int nameColor = (i == selectedIndex) ? COL_NAME_SEL : COL_NAME_IDLE;

            if (hasPortrait[i]) {
                guiGraphics.blit(portraits[i], px - 16, py - 16, 0, 0, 32, 32, 32, 32);
                int nx = (int) Math.round(cx + NAME_R * Math.sin(angle));
                int ny = (int) Math.round(cy - NAME_R * Math.cos(angle));
                guiGraphics.drawCenteredString(this.font, chars[i].getDisplayName(), nx, ny - 4, nameColor);
            } else {
                guiGraphics.drawCenteredString(this.font, chars[i].getDisplayName(), px, py - 4, nameColor);
            }
        }

        // 가운데 확정 원
        boolean centerHover = distance(lmx, lmy, cx, cy) <= 22;
        guiGraphics.blit(CENTER, cx - 24, cy - 24, 0, centerHover ? 48 : 0, 48, 48, 48, 96);
        guiGraphics.drawCenteredString(this.font, chars[selectedIndex].getDisplayName(), cx, cy - 4, COL_NAME_SEL);

        // 오른쪽 설명란: 선택된 캐릭터 이름·능력치·스킬 설명
        CharacterType selected = chars[selectedIndex];
        guiGraphics.drawString(this.font, selected.getDisplayName(), INFO_X, 18, COL_NAME_SEL);
        int maxHealth = (int) CharacterStats.getMaxHealth(selected);
        int maxStamina = CharacterStats.getMaxStamina(selected);
        String staminaText = maxStamina >= 1000 ? "무제한" : String.valueOf(maxStamina);
        guiGraphics.drawString(this.font, "체력 " + maxHealth, INFO_X, 36, 0xE24B4A);
        guiGraphics.drawString(this.font, "스태미나 " + staminaText, INFO_X, 48, 0x8FBF4D);
        // 설명이 칸 폭을 넘으면 자동 줄바꿈
        int wrapWidth = LOGICAL_W - INFO_X - 6;
        int lineY = 68;
        for (String line : selected.getDescription()) {
            for (net.minecraft.util.FormattedCharSequence seq
                    : this.font.split(net.minecraft.network.chat.FormattedText.of(line), wrapWidth)) {
                guiGraphics.drawString(this.font, seq, INFO_X, lineY, COL_NAME_IDLE);
                lineY += 11;
            }
            lineY += 1;
        }

        guiGraphics.drawCenteredString(this.font, "← → 회전 · Enter 확정", cx, LOGICAL_H - 14, COL_HINT);

        // 남은 시간 카운트다운 (5초 이하면 빨갛게)
        int secondsLeft = (autoPickLeft + 19) / 20;
        guiGraphics.drawCenteredString(this.font, secondsLeft + "초 후 무작위 선택",
                cx, LOGICAL_H - 26, secondsLeft <= 5 ? 0xFF5050 : COL_HINT);

        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // 휠 텍스처를 중심 기준으로 회전시켜 그리기 (논리 좌표계 안에서 사용)
    private void drawWheelRotated(GuiGraphics guiGraphics, ResourceLocation texture, float degrees, int cx, int cy) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(cx, cy, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        guiGraphics.pose().translate(-WHEEL_SIZE / 2f, -WHEEL_SIZE / 2f, 0);
        guiGraphics.blit(texture, 0, 0, 0, 0, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE);
        guiGraphics.pose().popPose();
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // 논리 좌표 → 화면상 부채꼴 칸 번호 (12시=0, 시계방향 0~SLOTS-1). 휠 밖이면 -1
    private int slotAt(double lx, double ly, int cx, int cy) {
        double dx = lx - cx, dy = ly - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        if (r < R_IN || r > R_OUT) {
            return -1;
        }
        double angleDeg = Math.toDegrees(Math.atan2(dx, -dy));
        return Math.floorMod((int) Math.round(angleDeg / SLOT_ANGLE), SLOTS);
    }

    // dir: +1 = 오른쪽(시계방향 다음) 캐릭터를 12시로, -1 = 왼쪽 캐릭터를 12시로
    private void rotate(int dir) {
        selectedIndex = Math.floorMod(selectedIndex + dir, chars.length);
        targetRot -= dir * SLOT_ANGLE;
    }

    private void confirm() {
        ModMessages.sendToServer(new SelectCharacterC2SPacket(chars[selectedIndex]));
        this.onClose();
    }

    // 매 틱: 제한시간 카운트다운 — 다 되면 무작위 캐릭터로 자동 확정
    @Override
    public void tick() {
        super.tick();
        if (--autoPickLeft <= 0) {
            selectedIndex = (int) (Math.random() * chars.length);
            confirm();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            rotate(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            rotate(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            updateLayout();
            double lx = toLogicalX(mouseX), ly = toLogicalY(mouseY);
            int cx = WHEEL_CX, cy = LOGICAL_H / 2;

            // 가운데 원 클릭 = 확정
            if (distance(lx, ly, cx, cy) <= 22) {
                confirm();
                return true;
            }

            // 부채꼴 클릭 = 그 자리의 캐릭터를 최단 경로로 12시까지 회전
            int slot = slotAt(lx, ly, cx, cy);
            if (slot >= 0) {
                double angleDeg = Math.toDegrees(Math.atan2(lx - cx, -(ly - cy)));
                int charIndex = Math.floorMod((int) Math.round((angleDeg - rot) / SLOT_ANGLE), chars.length);
                int diff = Math.floorMod(charIndex - selectedIndex, chars.length);
                if (diff > chars.length / 2) {
                    diff -= chars.length;
                }
                selectedIndex = charIndex;
                targetRot -= diff * SLOT_ANGLE;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
