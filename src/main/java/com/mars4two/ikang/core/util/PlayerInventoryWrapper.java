package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import net.minecraft.core.NonNullList;

import java.util.UUID;

public interface PlayerInventoryWrapper {
    UUID afkraft_companion$getPlayerId();
    NonNullList<RawItemStack> afkraft_companion$getInventoryItems();
    NonNullList<RawItemStack> afkraft_companion$getPlayerEquipment();
}
