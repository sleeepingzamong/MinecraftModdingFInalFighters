package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class ClientCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /캐릭터선택 또는 /character → 캐릭터 선택창 열기
        // 주의: 커맨드 실행 직후엔 채팅창이 아직 열려 있어서 바로 setScreen 하면 덮여버림.
        //       tell()로 다음 프레임에 열도록 미뤄야 함.
        dispatcher.register(Commands.literal("캐릭터선택").executes(context -> {
            Minecraft.getInstance().tell(ClientHooks::openCharacterSelectScreen);
            return 1;
        }));

        dispatcher.register(Commands.literal("character").executes(context -> {
            Minecraft.getInstance().tell(ClientHooks::openCharacterSelectScreen);
            return 1;
        }));
    }
}
