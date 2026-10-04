package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawInventory;
import com.mars4two.ikang.core.util.PlayerInventoryWrapper;

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

    public static PlayerInventorySnapshot of(PlayerInventoryWrapper inventory) {

        return new PlayerInventorySnapshot(
                Set.of(inventory.afkraft_companion$getPlayerId()),
                new RawInventory(inventory.afkraft_companion$getInventoryItems())
        );
    }
}
