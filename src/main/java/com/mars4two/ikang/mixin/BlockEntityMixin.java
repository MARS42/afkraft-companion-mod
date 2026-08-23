package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {

    @Inject(at = @At("TAIL"), method = "setChanged()V")
    private void onSetChanged(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;

        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        if (!(self instanceof BaseContainerBlockEntity container)) return;

        var config = AFKContainerConfigData.getConfig(level.getServer());
        if(config == null) return;
        if(!config.isContainerEnabledAt(level.dimension().identifier(), container.getBlockPos())) return;

        SnapshotIngestManager.getInstance().ingestContainerBlockEntity(container);
    }
}
