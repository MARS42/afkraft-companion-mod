package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.core.util.PlayerInventoryWrapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PlayerHotbarSnapshot(
        Set<UUID> playerIds,
        List<RawItemStack> items,
        int selectedSlot
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
        return "" + items + selectedSlot;
    }

    @Override
    public String data() {
        StringBuilder content = new StringBuilder("[");

        content.append("\"s=").append(selectedSlot).append("\",");
        content.append("\"_[");
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    content.append(",");
                }
                content.append("'").append(items.get(i).toPayloadString()).append("'");
            }
        }
        content.append("]_\"]");
        return content.toString();
    }

    public static PlayerHotbarSnapshot of(PlayerInventoryWrapper inventory) {

        return new PlayerHotbarSnapshot(
                Set.of(inventory.afkraft_companion$getPlayerId()),
                inventory.afkraft_companion$getHotbarItems(),
                inventory.afkraft_companion$selectedSlot()
        );
    }
}
