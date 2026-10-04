package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawHotbar;
import com.mars4two.ikang.core.util.PlayerInventoryWrapper;

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

    public static PlayerHotbarSnapshot of(PlayerInventoryWrapper inventory) {

        return new PlayerHotbarSnapshot(
                Set.of(inventory.afkraft_companion$getPlayerId()),
                new RawHotbar(inventory.afkraft_companion$selectedSlot(), inventory.afkraft_companion$getHotbarItems())
        );
    }
}
