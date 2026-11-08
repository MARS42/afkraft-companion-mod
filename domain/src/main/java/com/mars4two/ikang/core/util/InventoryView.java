package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Pure domain read-port over a player's inventory, hotbar, and armor equipment.
 */
public interface InventoryView {
    UUID playerId();
    List<RawItemStack> hotbarItems();
    List<RawItemStack> inventoryItems();
    List<RawItemStack> playerEquipment();
    int armorValue();
    int selectedSlot();
}
