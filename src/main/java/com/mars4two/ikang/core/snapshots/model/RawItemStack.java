package com.mars4two.ikang.core.snapshots.model;

import com.mars4two.ikang.core.util.MinecraftIdsUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public record RawItemStack(
        String id,
        String customName,
        int count,
        List<RawEnchantment> enchantments,
        int index,
        boolean isStackable
) implements RawObject {
    public static RawItemStack EMPTY = new RawItemStack("", null,0, List.of(), 0, false);

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
        String id = (item instanceof BlockItem bi)
                ? MinecraftIdsUtils.blockAlias(BuiltInRegistries.BLOCK.getKey(bi.getBlock()).toString())
                : MinecraftIdsUtils.itemAlias(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString());

        return new RawItemStack(
                id,
                customName,
                itemStack.getCount(),
                enchantments,
                slot,
                itemStack.isStackable()
        );
    }

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                id,
                customName,
                count,
                enchantments,
                index,
                isStackable
        };
    }
}
