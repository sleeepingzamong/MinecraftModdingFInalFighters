package com.example.examplemod.client;

import com.example.examplemod.combat.CharacterType;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.SelectCharacterC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CharacterSelectScreen extends Screen {

    private CharacterType selectedCharacter = CharacterType.NONE;
    private String selectedName = "선택 안 됨";

    public CharacterSelectScreen() {
        super(Component.literal("캐릭터 선택"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 90;            // 첫 줄 버튼 y
        int btnW = 110, btnH = 30;  // 버튼 크기
        int gapX = 10, gapY = 5;    // 버튼 간격
        int cols = 2;               // 한 줄에 2개

        // NONE을 뺀 모든 캐릭터를 격자로 자동 배치.
        // CharacterType enum에 캐릭터를 추가하면 여기 버튼도 자동으로 생김.
        int index = 0;
        for (CharacterType type : CharacterType.values()) {
            if (type == CharacterType.NONE) {
                continue;
            }
            final CharacterType picked = type;   // 람다에서 쓰려면 final 변수 필요
            int col = index % cols;
            int row = index / cols;
            int x = centerX - btnW - gapX / 2 + col * (btnW + gapX);
            int y = startY + row * (btnH + gapY);

            this.addRenderableWidget(Button.builder(
                    Component.literal(picked.getDisplayName()),
                    button -> {
                        selectedCharacter = picked;
                        selectedName = picked.getDisplayName();
                    }
            ).bounds(x, y, btnW, btnH).build());
            index++;
        }


        this.addRenderableWidget(Button.builder(
                Component.literal("선택"),
                button -> {
                    if (selectedCharacter != CharacterType.NONE) {
                        ModMessages.sendToServer(new SelectCharacterC2SPacket(selectedCharacter));
                        this.minecraft.setScreen(null);
                    }
                }
        ).bounds(this.width - 90, this.height - 35, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        guiGraphics.drawCenteredString(
                this.font,
                "캐릭터 선택",
                this.width / 2,
                30,
                0xFFFFFF
        );

        guiGraphics.drawCenteredString(
                this.font,
                "현재 선택: " + selectedName,
                this.width / 2,
                60,
                0xFFFF00
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}