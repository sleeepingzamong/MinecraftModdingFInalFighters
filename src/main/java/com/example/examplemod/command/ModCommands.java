package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ModMessages;
import com.example.examplemod.network.OpenSelectScreenS2CPacket;
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
// 클라이언트 커맨드(/캐릭터선택)와 달리 커맨드 블록·데이터팩에서 쓸 수 있다.
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(build("characterselect"));
        event.getDispatcher().register(build("캐릭터선택창"));
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
}
