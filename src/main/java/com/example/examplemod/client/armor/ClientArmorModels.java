package com.example.examplemod.client.armor;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

// 캐릭터 장비 커스텀 모델 보관소 (이름 → 구운 모델)
@OnlyIn(Dist.CLIENT)
public class ClientArmorModels {

    public static final String[] NAMES = {
            "monkey", "gambler", "swordsman", "hacker", "hammer", "cyborg", "berserker", "dojuk", "phoenix"
    };

    private static final Map<String, CharacterArmorModel> CACHE = new HashMap<>();

    public static ModelLayerLocation layerLocation(String name) {
        return new ModelLayerLocation(new ResourceLocation(ExampleMod.MODID, "armor_" + name), "main");
    }

    public static CharacterArmorModel get(String name) {
        return CACHE.computeIfAbsent(name, n -> new CharacterArmorModel(
                Minecraft.getInstance().getEntityModels().bakeLayer(layerLocation(n)),
                "dojuk".equals(n) || "hacker".equals(n)));   // 몸 장비인데 후드(머리 파츠)를 쓰는 것들
    }
}
