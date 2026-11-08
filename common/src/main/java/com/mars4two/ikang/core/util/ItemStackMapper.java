package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.platform.mc.InventoryEquipmentAdapter;
import net.minecraft.world.item.ItemStack;

public final class ItemStackMapper {

    private ItemStackMapper() {
    }

    public static RawItemStack from(int slot, ItemStack itemStack) {
        return InventoryEquipmentAdapter.mapItemStack(slot, itemStack);
    }
}
