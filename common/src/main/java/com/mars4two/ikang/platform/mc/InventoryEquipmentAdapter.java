package com.mars4two.ikang.platform.mc;

import com.mars4two.ikang.core.snapshots.model.RawEnchantment;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * Isolates player inventory, armor equipment, item stack enchantment, and custom container name
 * API differences between Minecraft 1.21.1 and 26.2.
 */
public final class InventoryEquipmentAdapter {

    private InventoryEquipmentAdapter() {
    }

    public static RawItemStack mapItemStack(int slot, ItemStack itemStack) {
        Component customNameComponent = itemStack.get(DataComponents.CUSTOM_NAME);
        var customName = customNameComponent != null ? customNameComponent.getString() : "";

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
                ? WorldAndEntityAdapter.blockId(((BlockItem) item).getBlock())
                : WorldAndEntityAdapter.itemId(item);

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

    public static List<RawItemStack> extractHotbarItems(NonNullList<ItemStack> items) {
        int selectionSize = Inventory.getSelectionSize();
        List<RawItemStack> rawItems = new ArrayList<>(selectionSize);
        for (int i = 0; i < selectionSize; i++) {
            rawItems.add(mapItemStack(i, items.get(i)));
        }
        return rawItems;
    }

    public static List<RawItemStack> extractMainInventoryItems(NonNullList<ItemStack> items) {
        int selectionSize = Inventory.getSelectionSize();
        int size = Math.max(0, items.size() - selectionSize);
        List<RawItemStack> rawItems = new ArrayList<>(size);
        for (int i = selectionSize; i < items.size(); i++) {
            rawItems.add(mapItemStack(i, items.get(i)));
        }
        return rawItems;
    }

    public static List<RawItemStack> extractArmorEquipment(Player player) {
        if (player == null) {
            return List.of();
        }
        return List.of(
                mapItemStack(0, player.getItemBySlot(EquipmentSlot.CHEST)),
                mapItemStack(1, player.getItemBySlot(EquipmentSlot.HEAD)),
                mapItemStack(2, player.getItemBySlot(EquipmentSlot.LEGS)),
                mapItemStack(3, player.getItemBySlot(EquipmentSlot.FEET))
        );
    }

    public static int getSelectedHotbarSlot(Inventory inventory) {
        //? if >=1.21.5 {
        return inventory.getSelectedSlot();
        //?} else {
        /*return inventory.selected;
        *///?}
    }

    public static boolean isHotbarSlot(int slot) {
        return Inventory.isHotbarSlot(slot);
    }

    public static String getCustomContainerName(Nameable nameable) {
        if (nameable == null || !nameable.hasCustomName()) {
            return null;
        }
        //? if >=1.21.9 {
        return nameable.getPlainTextName();
        //?} else {
        /*var customName = nameable.getCustomName();
        return customName != null ? customName.getString() : null;
        *///?}
    }
}
