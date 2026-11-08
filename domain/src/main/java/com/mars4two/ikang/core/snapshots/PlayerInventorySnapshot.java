package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawInventory;
import com.mars4two.ikang.core.util.InventoryView;

import java.util.Set;
import java.util.UUID;

public record PlayerInventorySnapshot(
        Set<UUID> playerIds,
        RawInventory data
) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerInventory";
    }

    @Override
    public String key() {
        return getSnapshotName() + playerIds;
    }

    public static PlayerInventorySnapshot of(InventoryView inventory) {
        return new PlayerInventorySnapshot(
                Set.of(inventory.playerId()),
                new RawInventory("p", inventory.inventoryItems())
        );
    }
}
