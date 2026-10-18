package com.example.examplemod.client;

import net.minecraft.core.BlockPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 클라이언트가 기억하는 "로드된 하늘 블록 위치" (SkyBlockEntity가 등록/해제)
public class ClientSkyBlocks {

    private static final Set<BlockPos> POSITIONS = ConcurrentHashMap.newKeySet();

    public static void add(BlockPos pos) {
        POSITIONS.add(pos.immutable());
    }

    public static void remove(BlockPos pos) {
        POSITIONS.remove(pos);
    }

    public static Set<BlockPos> all() {
        return POSITIONS;
    }

    public static void clearAll() {
        POSITIONS.clear();
    }
}
