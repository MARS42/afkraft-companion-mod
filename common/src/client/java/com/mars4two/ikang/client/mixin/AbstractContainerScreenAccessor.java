package com.mars4two.ikang.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int leftPos();

    @Accessor("topPos")
    int topPos();

    @Accessor("imageWidth")
    int imageWidth();
}
