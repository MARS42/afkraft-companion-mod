package com.mars4two.ikang.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
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
        if (!(self instanceof Container container)) return;

        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        System.out.println("BlockEntityMixin: " + self.getType() + " changed:" + self.getClass().getName());
    }
}
