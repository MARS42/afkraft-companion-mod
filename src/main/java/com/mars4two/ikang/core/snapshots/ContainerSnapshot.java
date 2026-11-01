package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawContainer;
import com.mars4two.ikang.core.snapshots.model.RawContainerType;
import com.mars4two.ikang.core.util.BlockEntityWrapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ContainerSnapshot(
        Set<UUID> playerIds,
        RawContainer data,
        String conflationKey
) implements SnapshotBase {

    public ContainerSnapshot(Set<UUID> playerIds, RawContainer data) {
        this(playerIds, data, data.blockPos().toString());
    }

    @Override
    public String getSnapshotName() {
        return "container";
    }

    @Override
    public String key() {
        return conflationKey;
    }

    @Override
    public Object changeToken() {
        return List.of(data.blockPos(), data.items());
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

    public static ContainerSnapshot ofPortable(Set<UUID> playerIds, UUID containerId, BlockEntityWrapper containerBlockEntity) {
        return new ContainerSnapshot(
                playerIds,
                new RawContainer(
                        RawContainerType.portableContainer(containerBlockEntity.afkraft_companion$blockId(), containerId),
                        containerBlockEntity.afkraft_companion$blockPos(),
                        containerBlockEntity.afkraft_companion$containerName(),
                        containerBlockEntity.afkraft_companion$getItems()
                ),
                "portable:" + containerId
        );
    }
}
