package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.core.util.PlayerInventoryWrapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PlayerInventorySnapshot(
        Set<UUID> playerIds,
        List<RawItemStack> items
) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerInventory";
    }

    @Override
    public String key() {
        return getSnapshotName() + playerIds;
    }

    @Override
    public Object changeToken() {
        return items;
    }

    @Override
    public String data() {
        StringBuilder content = new StringBuilder("[");

        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    content.append(",");
                }
                content.append("\"").append(items.get(i).toPayloadString()).append("\"");
            }
        }
        content.append("]");
        return content.toString();
    }

    public static PlayerInventorySnapshot of(PlayerInventoryWrapper inventory) {

        return new PlayerInventorySnapshot(
                Set.of(inventory.afkraft_companion$getPlayerId()),
                inventory.afkraft_companion$getInventoryItems()
        );
    }
}
