package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawContainer;
import com.mars4two.ikang.core.util.BlockEntityWrapper;

import java.util.Set;
import java.util.UUID;

public record LoadStationSnapshot(
        Set<UUID> playerIds,
        RawContainer data
) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "load-station";
    }

    @Override
    public String key() {
        return "load-station:" + data.blockPos().toString();
    }

    @Override
    public Object changeToken() {
        return data.items();
    }

    public static LoadStationSnapshot of(Set<UUID> playerIds, BlockEntityWrapper containerBlockEntity) {
        return new LoadStationSnapshot(
                playerIds,
                new RawContainer(containerBlockEntity.afkraft_companion$type(),
                        containerBlockEntity.afkraft_companion$blockPos(),
                        containerBlockEntity.afkraft_companion$containerName(),
                        containerBlockEntity.afkraft_companion$getItems())
        );
    }
}
