package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawEnchantment;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public final class ItemStackMapper {

    private ItemStackMapper() {
        // Utility class, no instances allowed
    }

    public static RawItemStack from(int slot, ItemStack itemStack) {
        var customName = itemStack.getCustomName() != null ? itemStack.getCustomName().getString() : "";

        var enchantmentHolders = itemStack.getEnchantments().keySet();
        List<RawEnchantment> enchantments = new ArrayList<>(enchantmentHolders.size());
        for (Holder<Enchantment> holder : enchantmentHolders) {
            enchantments.add(new RawEnchantment(
                    holder.getRegisteredName(),
                    itemStack.getEnchantments().getLevel(holder)
            ));
        }

        var item = itemStack.getItem();
        boolean isBlockItem = item instanceof BlockItem;
        String rawId = isBlockItem
                ? BuiltInRegistries.BLOCK.getKey(((BlockItem) item).getBlock()).toString()
                : BuiltInRegistries.ITEM.getKey(item).toString();

        return RawItemStack.of(
                slot,
                rawId,
                isBlockItem,
                customName,
                itemStack.getCount(),
                enchantments,
                itemStack.isStackable()
        );
    }
}
