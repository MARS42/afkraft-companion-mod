package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawContainer;
import com.mars4two.ikang.core.util.BlockEntityWrapper;

import java.util.Set;
import java.util.UUID;

public record ContainerSnapshot(
        Set<UUID> playerIds,
        RawContainer data
) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "container";
    }

    @Override
    public String key() {
        return data.blockPos().toString();
    }

    public static ContainerSnapshot of(Set<UUID> playerIds, BlockEntityWrapper containerBlockEntity) {
        return new ContainerSnapshot(
                playerIds,
                new RawContainer(containerBlockEntity.afkraft_companion$type(),
                        containerBlockEntity.afkraft_companion$blockPos(),
                        containerBlockEntity.afkraft_companion$containerName(),
                        containerBlockEntity.afkraft_companion$getItems())
        );
    }
}
