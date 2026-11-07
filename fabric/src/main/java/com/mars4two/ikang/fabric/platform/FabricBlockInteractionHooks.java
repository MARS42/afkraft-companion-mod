package com.mars4two.ikang.fabric.platform;

import com.mars4two.ikang.platform.services.BlockInteractionHooks;
//? if >=1.21.11 {
import net.fabricmc.fabric.api.event.player.BlockEvents;
//?} else {
/*import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
*///?}
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FabricBlockInteractionHooks implements BlockInteractionHooks {

    @Override
    public void onBlockUse(BlockUseListener listener) {
        //? if >=1.21.11 {
        BlockEvents.USE_WITHOUT_ITEM.register((state, level, pos, player, hitResult) -> {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
                listener.onUse(serverPlayer, serverLevel, pos, state, hitResult);
            }
            return null;
        });
        //?} else {
        /*UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!level.isClientSide()
                    && hand == InteractionHand.MAIN_HAND
                    && !player.isSpectator()
                    && player instanceof ServerPlayer serverPlayer
                    && level instanceof ServerLevel serverLevel) {
                BlockPos pos = hitResult.getBlockPos();
                boolean hasHeldItem = !serverPlayer.getMainHandItem().isEmpty()
                        || !serverPlayer.getOffhandItem().isEmpty();
                if (!serverPlayer.isSecondaryUseActive() || !hasHeldItem) {
                    BlockState state = serverLevel.getBlockState(pos);
                    listener.onUse(serverPlayer, serverLevel, pos, state, hitResult);
                }
            }
            return InteractionResult.PASS;
        });
        *///?}
    }

    @Override
    public void onAfterBlockBreak(BlockBreakListener listener) {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
                listener.onBreak(serverPlayer, serverLevel, pos, state);
            }
        });
    }
}
