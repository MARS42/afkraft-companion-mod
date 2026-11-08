package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.events.ServerEvents;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mars4two.ikang.AFKraftCompanion.MOD_ID;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    private static final Logger log = LoggerFactory.getLogger(MOD_ID);

    @Inject(method = "useOn", at = @At("RETURN"))
    private void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!WorldAndEntityAdapter.isSuccessfulBlockPlacement(cir.getReturnValue())) {
            return;
        }
        if (context.getLevel().isClientSide()) {
            return;
        }
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var containerConfig = AFKContainerConfigData.getConfig(context.getLevel().getServer());
        var blockPos = context.getClickedPos();

        var placedBlockPos = blockPos.relative(context.getClickedFace());
        var blockState = context.getLevel().getBlockState(placedBlockPos);

        var positions = ServerEvents.resolveAfkBlockPositions(placedBlockPos, blockState);
        var isCombinedContainer = containerConfig.isContainerEnabledIn(serverPlayer, positions);
        if (isCombinedContainer) {
            log.info("Combined container found at {}", placedBlockPos);
            positions.forEach(pos -> containerConfig.set(serverPlayer, pos, true));
        }
    }
}
