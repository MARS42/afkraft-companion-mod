package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawHotbar;
import com.mars4two.ikang.core.util.InventoryView;

import java.util.Set;
import java.util.UUID;

public record PlayerHotbarSnapshot(
        Set<UUID> playerIds,
        RawHotbar data
) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerHotbar";
    }

    @Override
    public String key() {
        return getSnapshotName() + playerIds;
    }

    @Override
    public Object changeToken() {
        return "" + data.items() + data.selectedSlot();
    }

    public static PlayerHotbarSnapshot of(InventoryView inventory) {
        return new PlayerHotbarSnapshot(
                Set.of(inventory.playerId()),
                new RawHotbar(inventory.selectedSlot(), inventory.hotbarItems())
        );
    }
}
