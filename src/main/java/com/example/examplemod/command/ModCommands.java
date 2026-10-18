package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.combat.CharacterType;
import com.example.examplemod.combat.PlayerCharacterManager;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.OpenSelectScreenS2CPacket;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;

// 서버 커맨드 (커맨드 블록에서 사용 가능):
//   /characterselect          → 실행한 플레이어에게 선택창 열기
//   /characterselect @p       → 대상 플레이어(들)에게 선택창 열기
//   /characterremove          → 실행한 플레이어의 직업 제거 (무기 회수·체력 원복)
//   /characterremove @a       → 대상 플레이어(들)의 직업 제거
//   /deletecharacter          → /characterremove 와 동일 (별칭)
// 클라이언트 커맨드(/캐릭터선택)와 달리 커맨드 블록·데이터팩에서 쓸 수 있다.
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(build("characterselect"));
        event.getDispatcher().register(build("캐릭터선택창"));
        event.getDispatcher().register(buildRemove("characterremove"));
        event.getDispatcher().register(buildRemove("deletecharacter"));
        event.getDispatcher().register(buildRemove("직업제거"));
        event.getDispatcher().register(buildClear("clearstatus"));
        event.getDispatcher().register(buildClear("상태초기화"));
        event.getDispatcher().register(buildUltReset("removeultimate"));
        event.getDispatcher().register(buildUltReset("궁게이지제거"));
        event.getDispatcher().register(buildUltFill("fillultimate"));
        event.getDispatcher().register(buildUltFill("궁게이지충전"));
        // 바닐라 1.20.2의 /random 재현 (value·roll·reset — 표시는 액션바)
        event.getDispatcher().register(RandomCommand.build());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))   // 커맨드 블록 = 레벨 2
                .executes(context ->
                        open(List.of(context.getSource().getPlayerOrException())))
                .then(Commands.argument("대상", EntityArgument.players())
                        .executes(context ->
                                open(EntityArgument.getPlayers(context, "대상"))));
    }

    private static int open(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            ModMessages.sendToPlayer(new OpenSelectScreenS2CPacket(), player);
        }
        return players.size();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRemove(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .executes(context ->
                        remove(List.of(context.getSource().getPlayerOrException())))
                .then(Commands.argument("대상", EntityArgument.players())
                        .executes(context ->
                                remove(EntityArgument.getPlayers(context, "대상"))));
    }

    // 직업을 NONE으로 되돌림 — setCharacter가 체력·스태미나 리셋과 캐릭터 무기 회수까지 처리
    private static int remove(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            PlayerCharacterManager.setCharacter(player, CharacterType.NONE);
            player.sendSystemMessage(Component.literal("직업이 제거되었습니다."));
        }
        return players.size();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildClear(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .executes(context ->
                        clearStatus(List.of(context.getSource().getPlayerOrException())))
                .then(Commands.argument("대상", EntityArgument.players())
                        .executes(context ->
                                clearStatus(EntityArgument.getPlayers(context, "대상"))));
    }

    // 모든 상태 효과 제거 (스턴·키반전·버프·표식·빙결·물약 효과 등 — StatusCleanser가 처리)
    private static int clearStatus(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            com.example.examplemod.combat.StatusCleanser.clearAll(player);
            player.sendSystemMessage(Component.literal("모든 상태 효과가 제거되었습니다."));
        }
        return players.size();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildUltReset(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .executes(context ->
                        resetUltimate(List.of(context.getSource().getPlayerOrException())))
                .then(Commands.argument("대상", EntityArgument.players())
                        .executes(context ->
                                resetUltimate(EntityArgument.getPlayers(context, "대상"))));
    }

    // /fillultimate [대상] [양] — 궁극기 게이지 충전 (양 생략 시 가득)
    // 커맨드 블록 사용 예: 버튼 옆에 반복/충격 블록 → "fillultimate @p" (누른 플레이어를 가득 충전)
    private static LiteralArgumentBuilder<CommandSourceStack> buildUltFill(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .executes(context -> fillUltimate(
                        List.of(context.getSource().getPlayerOrException()),
                        com.example.examplemod.combat.UltimateGaugeManager.MAX_GAUGE))
                .then(Commands.argument("대상", EntityArgument.players())
                        .executes(context -> fillUltimate(
                                EntityArgument.getPlayers(context, "대상"),
                                com.example.examplemod.combat.UltimateGaugeManager.MAX_GAUGE))
                        .then(Commands.argument("양",
                                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,
                                                com.example.examplemod.combat.UltimateGaugeManager.MAX_GAUGE))
                                .executes(context -> fillUltimate(
                                        EntityArgument.getPlayers(context, "대상"),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "양")))));
    }

    private static int fillUltimate(Collection<ServerPlayer> players, int amount) {
        for (ServerPlayer player : players) {
            com.example.examplemod.combat.UltimateGaugeManager.addGauge(player, amount);
        }
        return players.size();
    }

    // 궁극기 게이지만 0으로 초기화
    private static int resetUltimate(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            com.example.examplemod.combat.UltimateGaugeManager.resetGauge(player);
            player.sendSystemMessage(Component.literal("궁극기 게이지가 초기화되었습니다."));
        }
        return players.size();
    }
}
