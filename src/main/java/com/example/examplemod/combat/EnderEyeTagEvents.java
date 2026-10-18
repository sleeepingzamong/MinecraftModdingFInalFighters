package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 엔더의 눈 우클릭 → 플레이어에게 "Eye" 태그 부여 (커맨드 블럭 @a[tag=Eye] 감지용)
// 바닐라 기능(요새 방향 던지기, 엔드 차원문 틀 끼우기)은 전부 차단
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class EnderEyeTagEvents {

    private static final String TAG = "Eye";

    // 허공/일반 블록 대상 우클릭 → 던지기 취소 + 태그
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(Items.ENDER_EYE)) {
            return;
        }
        event.setCanceled(true);   // 양쪽(서버+클라 예측)에서 취소해야 던지는 모션도 안 나옴
        if (!event.getLevel().isClientSide()) {
            event.getEntity().addTag(TAG);
        }
    }

    // 블록 우클릭 시 아이템 사용만 차단 (엔드 차원문 틀 끼우기 방지 — 상자·문 등 블록 상호작용은 그대로)
    // 블록이 상호작용을 안 받으면 위 RightClickItem으로 이어져 태그가 붙는다
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().is(Items.ENDER_EYE)) {
            event.setUseItem(Event.Result.DENY);
        }
    }
}
