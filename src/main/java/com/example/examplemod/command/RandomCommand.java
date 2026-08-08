package com.example.examplemod.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

// 바닐라 1.20.2의 /random을 1.20.1에 재현 (문법 동일):
//   /random value <최소..최대> [시퀀스]  → 조용히 뽑기 — /execute store result 로 스코어보드 저장 가능
//   /random roll <최소..최대> [시퀀스]   → 공개 굴림 (모두에게 표시)
//   /random reset <시퀀스|*> [시드]      → 시퀀스 초기화 (시드를 주면 재현 가능한 랜덤)
// 차이점: 모든 표시는 채팅 대신 "액션바" (채팅창에 아무것도 안 올라옴)
public class RandomCommand {

    private static final Map<ResourceLocation, RandomSource> SEQUENCES = new HashMap<>();
    private static final RandomSource DEFAULT = RandomSource.create();

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("random")
                .then(drawBranch("value", false))
                .then(drawBranch("roll", true))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("*")
                                .executes(context -> resetAll(context.getSource())))
                        .then(Commands.argument("시퀀스", ResourceLocationArgument.id())
                                .executes(context -> reset(context.getSource(),
                                        ResourceLocationArgument.getId(context, "시퀀스"), null))
                                .then(Commands.argument("시드", LongArgumentType.longArg())
                                        .executes(context -> reset(context.getSource(),
                                                ResourceLocationArgument.getId(context, "시퀀스"),
                                                LongArgumentType.getLong(context, "시드"))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> drawBranch(String name, boolean publicRoll) {
        return Commands.literal(name)
                .then(Commands.argument("범위", RangeArgument.intRange())
                        .executes(context -> draw(context, null, publicRoll))
                        .then(Commands.argument("시퀀스", ResourceLocationArgument.id())
                                .executes(context -> draw(context,
                                        ResourceLocationArgument.getId(context, "시퀀스"), publicRoll))));
    }

    private static int draw(CommandContext<CommandSourceStack> context, ResourceLocation sequence, boolean publicRoll) {
        CommandSourceStack source = context.getSource();
        MinMaxBounds.Ints range = RangeArgument.Ints.getRange(context, "범위");
        Integer min = range.getMin();
        Integer max = range.getMax();

        if (min == null || max == null) {
            feedback(source, "범위는 1..100 처럼 최소..최대를 모두 적어야 합니다.");
            return 0;
        }
        long size = (long) max - (long) min + 1L;
        if (size <= 0 || size > Integer.MAX_VALUE) {
            feedback(source, "잘못된 범위입니다.");
            return 0;
        }

        RandomSource random = sequence == null ? DEFAULT
                : SEQUENCES.computeIfAbsent(sequence, key -> RandomSource.create());
        int value = min + random.nextInt((int) size);

        if (publicRoll) {
            // 공개 굴림 — 모두의 액션바에 표시 (채팅 X)
            Component message = Component.literal(
                    source.getTextName() + "의 굴림: " + value + " (" + min + "~" + max + ")");
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                player.displayClientMessage(message, true);
            }
        } else {
            feedback(source, "랜덤: " + value + " (" + min + "~" + max + ")");
        }
        return value;   // /execute store result score ... run random value 1..6 → 이 값이 저장됨
    }

    private static int reset(CommandSourceStack source, ResourceLocation sequence, Long seed) {
        SEQUENCES.put(sequence, seed == null ? RandomSource.create() : RandomSource.create(seed));
        feedback(source, "시퀀스 초기화: " + sequence);
        return 1;
    }

    private static int resetAll(CommandSourceStack source) {
        int count = SEQUENCES.size();
        SEQUENCES.clear();
        feedback(source, "시퀀스 " + count + "개 초기화");
        return count;
    }

    // 채팅 대신 액션바 (실행자가 플레이어일 때만 — 커맨드 블록·콘솔이면 조용히)
    private static void feedback(CommandSourceStack source, String text) {
        if (source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.literal(text), true);
        }
    }
}
