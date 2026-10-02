package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.core.util.PlayerInventoryWrapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PlayerEquipmentSnapshot(
        Set<UUID> playerIds,
        List<RawItemStack> equipment
) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerEquipment";
    }

    @Override
    public String key() {
        return getSnapshotName() + playerIds;
    }

    @Override
    public Object changeToken() {
        return equipment;
    }

    @Override
    public String data() {
        StringBuilder content = new StringBuilder("[");

        if (equipment != null && !equipment.isEmpty()) {
            for (int i = 0; i < equipment.size(); i++) {
                if (i > 0) {
                    content.append(",");
                }
                content.append(equipment.get(i).toPayloadString());
            }
        }

        content.append("]");
        return content.toString();
    }

    public static PlayerEquipmentSnapshot of(PlayerInventoryWrapper inventory) {

        return new PlayerEquipmentSnapshot(
                Set.of(inventory.afkraft_companion$getPlayerId()),
                inventory.afkraft_companion$getPlayerEquipment()
        );
    }
}
