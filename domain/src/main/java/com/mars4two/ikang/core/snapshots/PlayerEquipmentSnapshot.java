package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawEquipment;
import com.mars4two.ikang.core.util.InventoryView;

import java.util.Set;
import java.util.UUID;

public record PlayerEquipmentSnapshot(
        Set<UUID> playerIds,
        RawEquipment data
) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerEquipment";
    }

    @Override
    public String key() {
        return getSnapshotName() + playerIds;
    }

    public static PlayerEquipmentSnapshot of(InventoryView inventory) {
        return new PlayerEquipmentSnapshot(
                Set.of(inventory.playerId()),
                new RawEquipment(
                        inventory.playerEquipment(),
                        inventory.armorValue()
                )
        );
    }
}
