package com.example.examplemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {

    public static final String CATEGORY = "key.categories.examplemod";

    public static final KeyMapping SKILL_1_KEY = new KeyMapping(
            "key.examplemod.skill_1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY
    );

    public static final KeyMapping SKILL_2_KEY = new KeyMapping(
            "key.examplemod.skill_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );

    public static final KeyMapping ULTIMATE_KEY = new KeyMapping(
            "key.examplemod.ultimate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );
    public static final KeyMapping DASH_KEY = new KeyMapping(
        "key.examplemod.dash",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_CONTROL,
        CATEGORY
        );
        public static final KeyMapping GUARD_KEY = new KeyMapping(
        "key.examplemod.guard",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_F,
        CATEGORY
);
}