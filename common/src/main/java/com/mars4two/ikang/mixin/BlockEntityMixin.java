package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.snapshots.model.RawContainerType;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import com.mars4two.ikang.core.util.BlockEntityWrapper;
import com.mars4two.ikang.core.util.ContainerView;
import com.mars4two.ikang.core.util.PortableContainerIdentityHelper;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
//? if >=1.21.5 {
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements BlockEntityWrapper {

    @Unique
    private static final SnapshotIngestManager ingestManager = SnapshotIngestManager.getInstance();

    @Unique
    private ContainerView afkraft_companion$cachedView;

    @Override
    public ContainerView afkraft_companion$asContainerView() {
        if (afkraft_companion$cachedView == null) {
            afkraft_companion$cachedView = new ContainerView() {
                @Override
                public RawContainerType type() {
                    return BlockEntityMixin.this.afkraft_companion$type();
                }

                @Override
                public Optional<UUID> containerId() {
                    return BlockEntityMixin.this.afkraft_companion$containerId();
                }

                @Override
                public BlockCoord blockCoord() {
                    return BlockEntityMixin.this.afkraft_companion$blockCoord();
                }

                @Override
                public String blockId() {
                    return BlockEntityMixin.this.afkraft_companion$blockId();
                }

                @Override
                public String containerName() {
                    return BlockEntityMixin.this.afkraft_companion$containerName();
                }

                @Override
                public List<RawItemStack> items() {
                    return BlockEntityMixin.this.afkraft_companion$getItems();
                }
            };
        }
        return afkraft_companion$cachedView;
    }

    @Inject(at = @At("TAIL"), method = "setChanged()V")
    private void onSetChanged(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;

        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        if (!(self instanceof BaseContainerBlockEntity)) return;

        var config = AFKContainerConfigData.getConfig(level.getServer());
        if (config == null) return;

        var dimensionId = WorldAndEntityAdapter.dimensionId(level);
        var blockPos = self.getBlockPos();
        var view = afkraft_companion$asContainerView();

        var containerIdOpt = PortableContainerIdentityHelper.getContainerId(self);
        if (containerIdOpt.isPresent()) {
            UUID containerId = containerIdOpt.get();
            if (config.isPortableContainerEnabled(containerId)) {
                ingestManager.ingestPortableContainerBlockEntity(
                        config.getPortableContainerOwners(containerId),
                        containerId,
                        view
                );
            }
        }

        if (config.isLoadStationEnabledAt(dimensionId, blockPos)) {
            ingestManager.ingestLoadStationBlockEntity(
                    config.getLoadStationOwnersAt(dimensionId, blockPos),
                    view
            );
        }

        if (config.isContainerEnabledAt(dimensionId, blockPos)) {
            ingestManager.ingestContainerBlockEntity(
                    config.getEnabledOwnersAt(dimensionId, blockPos),
                    view
            );
        }
    }

    //? if >=1.21.5 {
    @Inject(at = @At("HEAD"), method = "preRemoveSideEffects")
    private void onPreRemoveSideEffects(BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;
        if (!(self instanceof BaseContainerBlockEntity)) return;

        var config = AFKContainerConfigData.getConfig(level.getServer());
        if (config == null) return;

        var dimensionId = WorldAndEntityAdapter.dimensionId(level);
        var owners = config.getEnabledOwnersAt(dimensionId, pos);
        if (!owners.isEmpty()) {
            ingestManager.ingestContainerDeleted(owners, afkraft_companion$asContainerView());
        }
        config.deleteFixedContainersAt(dimensionId, pos);
    }
    //?}

    @Override
    public RawContainerType afkraft_companion$type() {
        var blockCoord = afkraft_companion$blockCoord();

        if ((Object) this instanceof ChestBlockEntity chestBlockEntity) {
            var chestType = chestBlockEntity.getBlockState().getValue(ChestBlock.TYPE);
            var connectedPos = chestBlockEntity.getBlockPos().relative(ChestBlock.getConnectedDirection(chestBlockEntity.getBlockState()));
            var connectedCoord = new BlockCoord(connectedPos.getX(), connectedPos.getY(), connectedPos.getZ());

            switch (chestType) {
                case LEFT:
                    return RawContainerType.doubleChest(connectedCoord, blockCoord);
                case RIGHT:
                    return RawContainerType.doubleChest(blockCoord, connectedCoord);
                case SINGLE:
                    return RawContainerType.singleChest();
            }
        }
        var type = afkraft_companion$blockId();
        return RawContainerType.ofType(type);
    }
}
