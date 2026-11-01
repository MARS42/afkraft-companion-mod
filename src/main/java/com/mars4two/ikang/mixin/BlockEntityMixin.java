package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.snapshots.model.RawContainerType;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import com.mars4two.ikang.core.util.BlockEntityWrapper;
import com.mars4two.ikang.core.util.PortableContainerIdentityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements BlockEntityWrapper {

    @Unique
    private static final SnapshotIngestManager ingestManager = SnapshotIngestManager.getInstance();

    @Inject(at = @At("TAIL"), method = "setChanged()V")
    private void onSetChanged(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;

        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        if (!(self instanceof BaseContainerBlockEntity)) return;

        var config = AFKContainerConfigData.getConfig(level.getServer());
        if (config == null) return;

        var dimensionId = level.dimension().identifier();
        var blockPos = self.getBlockPos();

        var containerIdOpt = PortableContainerIdentityHelper.getContainerId(self);
        if (containerIdOpt.isPresent()) {
            UUID containerId = containerIdOpt.get();
            if (config.isPortableContainerEnabled(containerId)) {
                ingestManager.ingestPortableContainerBlockEntity(
                        config.getPortableContainerOwners(containerId),
                        containerId,
                        this
                );
            }
        }

        if (config.isLoadStationEnabledAt(dimensionId, blockPos)) {
            ingestManager.ingestLoadStationBlockEntity(
                    config.getLoadStationOwnersAt(dimensionId, blockPos),
                    this
            );
        }

        if (config.isContainerEnabledAt(dimensionId, blockPos)) {
            ingestManager.ingestContainerBlockEntity(
                    config.getEnabledOwnersAt(dimensionId, blockPos),
                    this
            );
        }
    }

    @Inject(at = @At("HEAD"), method = "preRemoveSideEffects")
    private void onPreRemoveSideEffects(BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;
        if (!(self instanceof BaseContainerBlockEntity)) return;

        var config = AFKContainerConfigData.getConfig(level.getServer());
        if (config == null) return;

        var dimensionId = level.dimension().identifier();
        var owners = config.getEnabledOwnersAt(dimensionId, pos);
        if (!owners.isEmpty()) {
            ingestManager.ingestContainerDeleted(owners, this);
        }
        config.deleteFixedContainersAt(dimensionId, pos);
    }

    @Override
    public RawContainerType afkraft_companion$type() {
        var blockPos = afkraft_companion$blockPos();

        if ((Object) this instanceof ChestBlockEntity chestBlockEntity) {
            var chestType = chestBlockEntity.getBlockState().getValue(ChestBlock.TYPE);
            var connectedPos = ChestBlock.getConnectedBlockPos(chestBlockEntity.getBlockPos(), chestBlockEntity.getBlockState());

            switch (chestType) {
                case LEFT:
                    return RawContainerType.doubleChest(connectedPos, blockPos);
                case RIGHT:
                    return RawContainerType.doubleChest(blockPos, connectedPos);
                case SINGLE:
                    return RawContainerType.singleChest();
            }
        }
        var type = afkraft_companion$blockId();
        return RawContainerType.ofType(type);
    }
}
