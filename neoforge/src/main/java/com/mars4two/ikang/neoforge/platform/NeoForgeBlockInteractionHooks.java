package com.mars4two.ikang.neoforge.platform;

import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import com.mars4two.ikang.platform.services.BlockInteractionHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class NeoForgeBlockInteractionHooks implements BlockInteractionHooks {

    @Override
    public void onBlockUse(BlockUseListener listener) {
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                false,
                PlayerInteractEvent.RightClickBlock.class,
                event -> {
                    if (event.getHand() == InteractionHand.MAIN_HAND
                            && event.getLevel() instanceof ServerLevel serverLevel
                            && event.getEntity() instanceof ServerPlayer serverPlayer) {
                        BlockPos pos = event.getPos();
                        boolean hasHeldItem = !serverPlayer.getMainHandItem().isEmpty()
                                || !serverPlayer.getOffhandItem().isEmpty();
                        boolean suppressUsingBlock = serverPlayer.isSecondaryUseActive()
                                && hasHeldItem
                                && !(serverPlayer.getMainHandItem().doesSneakBypassUse(serverLevel, pos, serverPlayer)
                                && serverPlayer.getOffhandItem().doesSneakBypassUse(serverLevel, pos, serverPlayer));
                        if (event.getUseBlock().isTrue() || (event.getUseBlock().isDefault() && !suppressUsingBlock)) {
                            BlockState state = serverLevel.getBlockState(pos);
                            listener.onUse(serverPlayer, serverLevel, pos, state, event.getHitVec());
                        }
                    }
                }
        );
    }

    @Override
    public void onAfterBlockBreak(BlockBreakListener listener) {
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                false,
                BreakBlockEvent.class,
                event -> {
                    if (event.getLevel() instanceof ServerLevel serverLevel
                            && event.getPlayer() instanceof ServerPlayer serverPlayer) {
                        BlockState brokenState = event.getState();
                        handleBreakEvent(
                                task -> WorldAndEntityAdapter.scheduleNextServerTask(serverLevel.getServer(), task),
                                immutablePos -> !serverLevel.getBlockState(immutablePos).is(brokenState.getBlock()),
                                event.getPos(),
                                immutablePos -> listener.onBreak(serverPlayer, serverLevel, immutablePos, brokenState)
                        );
                    }
                }
        );
    }

    public static void handleBreakEvent(
            Consumer<Runnable> postBreakScheduler,
            Predicate<BlockPos> wasBlockRemovedAtPos,
            BlockPos eventPos,
            Consumer<BlockPos> onConfirmedBreak
    ) {
        BlockPos immutablePos = eventPos.immutable();
        postBreakScheduler.accept(() -> {
            if (wasBlockRemovedAtPos.test(immutablePos)) {
                onConfirmedBreak.accept(immutablePos);
            }
        });
    }
}
