package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.CharacterArmorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 캐릭터 전용 장비 유지 담당:
//  - 2초마다 착용 확인 → 벗거나 인벤토리로 옮겼으면 강제 재착용 (복사본 정리 포함)
//  - 죽어도 전용 장비는 바닥에 떨어지지 않음 (리스폰 시 재지급)
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class CharacterArmorEvents {

    // 던지기가 감지된 플레이어 — 클릭 처리가 끝난 "다음 틱"에 재착용
    // (이벤트 도중에 바로 껴주면 클릭 마무리 동기화가 덮어써서 상태가 꼬임)
    private static final java.util.Set<java.util.UUID> PENDING_ENSURE = new java.util.HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        // 던지기 직후 재착용 (다음 틱) + 클라이언트 인벤토리 강제 동기화
        if (PENDING_ENSURE.remove(player.getUUID())) {
            PlayerCharacterManager.ensureCharacterArmor(player);
            player.inventoryMenu.broadcastChanges();
        }
        if (player.tickCount % 10 != 0) {
            return;   // 0.5초마다 검사 (복제 시도 틈을 최소화)
        }
        PlayerCharacterManager.ensureCharacterArmor(player);
    }

    // 상자 등 다른 보관함을 닫는 순간, 그 보관함에 넣어둔 전용 장비를 회수(삭제)
    @SubscribeEvent
    public static void onContainerClose(net.minecraftforge.event.entity.player.PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (net.minecraft.world.inventory.Slot slot : event.getContainer().slots) {
            if (slot.container != player.getInventory()
                    && slot.getItem().getItem() instanceof CharacterArmorItem) {
                slot.set(net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            event.getDrops().removeIf(drop -> drop.getItem().getItem() instanceof CharacterArmorItem);
        }
    }

    // 전용 장비 던지기 차단 (Q키·인벤토리 화면 던지기 모두) — 벗는 동시에 버리는 복제 방지
    @SubscribeEvent
    public static void onItemToss(net.minecraftforge.event.entity.item.ItemTossEvent event) {
        if (event.getEntity().getItem().getItem() instanceof CharacterArmorItem) {
            event.setCanceled(true);   // 바닥에 안 떨어짐 (버린 복사본은 소멸)
            if (event.getPlayer() instanceof ServerPlayer player) {
                PENDING_ENSURE.add(player.getUUID());   // 다음 틱에 재착용 (틱 핸들러가 처리)
            }
        }
    }

    // 어떤 경로로든(디스펜서 등) 전용 장비가 바닥 아이템으로 생기는 것 자체를 차단
    @SubscribeEvent
    public static void onEntityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
                && item.getItem().getItem() instanceof CharacterArmorItem) {
            event.setCanceled(true);
        }
    }
}
