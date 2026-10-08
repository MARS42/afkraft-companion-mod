package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import com.mars4two.ikang.core.util.BlockEntityWrapper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


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
        if(config == null) return;
        var dimensionId = level.dimension().identifier();
        var blockPos = self.getBlockPos();
        if(!config.isContainerEnabledAt(dimensionId, blockPos)) return;

        ingestManager.ingestContainerBlockEntity(config.getEnabledOwnersAt(dimensionId, blockPos), this);
    }

    @Override
    public String afkraft_companion$type() {
        if((Object) this instanceof ChestBlockEntity chestBlockEntity){
            var chestType = chestBlockEntity.getBlockState().getValue(ChestBlock.TYPE);
            switch (chestType) {
                case LEFT:
                    return "lc";
                case RIGHT:
                    return "rc";
                case SINGLE:
                    return "c";
            }
        }
        return "c";
    }

    @Override
    public String afkraft_companion$defaultName() {
        return "Nombre default";
    }
}
