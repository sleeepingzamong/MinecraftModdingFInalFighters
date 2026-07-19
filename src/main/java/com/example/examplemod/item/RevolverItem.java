package com.example.examplemod.item;

import com.example.examplemod.capability.PlayerCombatDataProvider;
import com.example.examplemod.combat.CombatSync;
import com.example.examplemod.combat.CommonCombatActions;
import com.example.examplemod.combat.PlayerCombatData;
import com.example.examplemod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// 도박사 리볼버: 좌클릭 발사(6연발), 데미지 매 발 랜덤, 6발 소진 시 자동 재장전
public class RevolverItem extends Item {

    public static final int MAX_AMMO = 6;
    private static final int SHOT_COOLDOWN = 8;      // 연사 간격 (0.4초)
    private static final int RELOAD_TICKS = 60;      // 재장전 (3초)
    private static final int MIN_DAMAGE = 1;         // 최소 데미지
    private static final int MAX_DAMAGE = 7;         // 최대 데미지
    private static final double RANGE = 30.0;        // 총알 사거리(칸)
    private static final float BULLET_SPEED = 3.0F;  // 총알 속도 (기존 투사체 1.5의 2배)

    public RevolverItem(Properties properties) {
        super(properties);
    }

    // ----- 탄약 (총 아이템의 NBT에 저장 — 자동으로 클라 동기화됨) -----
    public static int getAmmo(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("ammo")) {
            return stack.getTag().getInt("ammo");
        }
        return MAX_AMMO;   // 처음 만든 총은 가득 찬 상태
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        stack.getOrCreateTag().putInt("ammo", ammo);
    }

    // 좌클릭 패킷이 도착하면 서버에서 실행 (검증 → 미리 굴린 데미지로 발사 → 다음 값 굴림 → 탄약/재장전)
    public static void tryShoot(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof RevolverItem revolver)) {
            return;   // 총을 안 들고 있으면 무시 (해킹 클라 방어)
        }
        if (player.getCooldowns().isOnCooldown(revolver)) {
            return;   // 연사 간격/재장전 중
        }
        if (com.example.examplemod.combat.GamblerEvents.isRouletteLocked(player)) {
            return;   // 룰렛 연출 중엔 발사 불가
        }

        player.getCapability(PlayerCombatDataProvider.PLAYER_COMBAT_DATA).ifPresent(data -> {
            if (data.getNextGunDamage() <= 0) {
                rollNextDamage(player, data);   // 안전장치 (보통은 이미 굴려져 있음)
            }

            // UI에 보여주던 "다음 탄환" 값 그대로 발사
            float damage = data.getNextGunDamage();
            CommonCombatActions.fireProjectile(player, RANGE,
                    new ItemStack(ModItems.BULLET.get()), damage, 1.0F, BULLET_SPEED);

            rollNextDamage(player, data);   // 다음 탄환 값을 새로 굴림

            int ammo = getAmmo(stack) - 1;
            if (ammo <= 0) {
                setAmmo(stack, MAX_AMMO);   // 자동 재장전 (아래 쿨다운이 발사를 막음)
                player.getCooldowns().addCooldown(revolver, RELOAD_TICKS);
                // 재장전 끝나는 시각을 NBT에 기록 → 클라가 이걸 보고 애니메이션 진행도를 계산
                stack.getOrCreateTag().putLong("reloadEnd", player.level().getGameTime() + RELOAD_TICKS);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 0.6F, 1.4F);
            } else {
                setAmmo(stack, ammo);
                player.getCooldowns().addCooldown(revolver, SHOT_COOLDOWN);
            }
        });

        CombatSync.syncStats(player);   // "다음 탄환" UI 갱신
    }

    private static void rollNextDamage(ServerPlayer player, PlayerCombatData data) {
        data.setNextGunDamage(MIN_DAMAGE + player.getRandom().nextInt(MAX_DAMAGE - MIN_DAMAGE + 1));
    }

    // 재장전 진행도 (클라 모델 교체용): 0 = 재장전 아님, 0~1 = 진행 중
    public static float getReloadProgress(ItemStack stack, @Nullable LivingEntity entity) {
        if (entity == null || !stack.hasTag() || !stack.getTag().contains("reloadEnd")) {
            return 0F;
        }
        long remaining = stack.getTag().getLong("reloadEnd") - entity.level().getGameTime();
        if (remaining <= 0 || remaining > RELOAD_TICKS) {
            return 0F;   // 이미 끝났거나 값이 이상하면 기본 모델
        }
        return 1.0F - (float) remaining / RELOAD_TICKS;
    }

    // 총을 든 순간부터 UI가 보이도록: 아직 안 굴렸으면 첫 값을 굴림 (PlayerTickHub가 매 틱 호출)
    public static void ensureNextDamageRolled(ServerPlayer player, PlayerCombatData data) {
        if (data.getNextGunDamage() <= 0
                && player.getMainHandItem().getItem() instanceof RevolverItem) {
            rollNextDamage(player, data);
            CombatSync.syncStats(player);
        }
    }

    // ----- 탄약 표시: 내구도 바를 탄약 바로 재활용 (금색) -----
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;   // 항상 표시
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getAmmo(stack) / MAX_AMMO);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFD700;   // 금색
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("도박사의 전용 무기").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("좌클릭 발사 — 데미지는 운에 맡겨라 (1~7)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("탄약 " + getAmmo(itemStack) + "/" + MAX_AMMO).withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}
