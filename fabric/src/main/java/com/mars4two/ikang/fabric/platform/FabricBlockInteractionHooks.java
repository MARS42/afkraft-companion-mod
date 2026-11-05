package com.mars4two.ikang.fabric.platform;

import com.mars4two.ikang.platform.services.BlockInteractionHooks;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FabricBlockInteractionHooks implements BlockInteractionHooks {

    @Override
    public void onBlockUse(BlockUseListener listener) {
        BlockEvents.USE_WITHOUT_ITEM.register((state, level, pos, player, hitResult) -> {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
                listener.onUse(serverPlayer, serverLevel, pos, state, hitResult);
            }
            return null;
        });
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
