package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {

    @Unique
    private static final SnapshotIngestManager ingestManager = SnapshotIngestManager.getInstance();

    @Inject(at = @At("TAIL"), method = "setChanged()V")
    private void onSetChanged(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;

        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        if (!(self instanceof BaseContainerBlockEntity container)) return;

        var config = AFKContainerConfigData.getConfig(level.getServer());
        if(config == null) return;
        var dimensionId = level.dimension().identifier();
        if(!config.isContainerEnabledAt(dimensionId, container.getBlockPos())) return;

        ingestManager.ingestContainerBlockEntity(config.getEnabledOwnersAt(dimensionId, container.getBlockPos()), container);
    }
}
