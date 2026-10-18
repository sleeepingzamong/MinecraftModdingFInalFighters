package com.example.examplemod.combat;

import com.example.examplemod.ExampleMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 임시 디스플레이(갬블러 카드·룰렛 릴, 도적 표식 단검 등) 고아 청소부.
// 정상 경로는 각 매니저가 즉시 제거하지만, ID 조회 실패(청크 언로드·로그아웃·차원 이동 등)로
// 남겨진 표시가 영영 떠 있지 않게 — 스폰 시 새긴 만료 시각이 지나면 5초 주기 청소에서 소멸한다.
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class TempDisplaySweeper {

    public static final String TAG = "ff_temp_display";
    private static final String EXPIRE_KEY = "ff_expire";
    private static final int SWEEP_INTERVAL = 100;   // 5초마다

    // 디스플레이 엔티티에 "언제 지나면 버려도 되는지" 도장 찍기 (스폰 직후 호출)
    public static void mark(Entity display, long expireGameTime) {
        display.addTag(TAG);
        display.getPersistentData().putLong(EXPIRE_KEY, expireGameTime);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (now % SWEEP_INTERVAL != 0) {
            return;
        }
        level.getEntities(EntityType.ITEM_DISPLAY,
                e -> e.getTags().contains(TAG) && e.getPersistentData().getLong(EXPIRE_KEY) < now)
                .forEach(Entity::discard);
        level.getEntities(EntityType.TEXT_DISPLAY,
                e -> e.getTags().contains(TAG) && e.getPersistentData().getLong(EXPIRE_KEY) < now)
                .forEach(Entity::discard);
    }
}
