package com.example.examplemod.client;

import net.minecraft.client.Minecraft;

public class ClientHooks {

    public static void openCharacterSelectScreen() {
        Minecraft.getInstance().setScreen(new CharacterSelectScreen());
    }
}