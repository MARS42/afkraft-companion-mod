package com.mars4two.ikang.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public interface BlockInteractionHooks {

    @FunctionalInterface
    interface BlockUseListener {
        void onUse(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, BlockHitResult hitResult);
    }

    @FunctionalInterface
    interface BlockBreakListener {
        void onBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state);
    }

    void onBlockUse(BlockUseListener listener);

    void onAfterBlockBreak(BlockBreakListener listener);
}
