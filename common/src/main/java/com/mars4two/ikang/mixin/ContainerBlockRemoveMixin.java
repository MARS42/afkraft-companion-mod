package com.mars4two.ikang.mixin;

//? if <26.2 {
/*import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import com.mars4two.ikang.core.util.BlockEntityWrapper;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Emits {@code CONTAINER_DELETED} snapshots on Minecraft 1.21.x before {@code Block.onRemove}
 * drops and clears {@code BaseContainerBlockEntity} items (26.2+ uses {@code BlockEntity.preRemoveSideEffects}).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ContainerBlockRemoveMixin {

    //? if <26.2 {
    /*@Shadow
    protected abstract BlockState asState();

    @Inject(method = "onRemove", at = @At("HEAD"))
    private void onBeforeContainerBlockRemoved(
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston,
            CallbackInfo ci
    ) {
        BlockState oldState = this.asState();
        if (oldState.is(newState.getBlock()) || level == null || level.isClientSide()) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BaseContainerBlockEntity) || !(blockEntity instanceof BlockEntityWrapper wrapper)) {
            return;
        }
        var config = AFKContainerConfigData.getConfig(level.getServer());
        if (config == null) {
            return;
        }
        var dimensionId = WorldAndEntityAdapter.dimensionId(level);
        var owners = config.getEnabledOwnersAt(dimensionId, pos);
        if (!owners.isEmpty()) {
            SnapshotIngestManager.getInstance().ingestContainerDeleted(owners, wrapper.afkraft_companion$asContainerView());
        }
        config.deleteFixedContainersAt(dimensionId, pos);
    }
    *///?}
}
