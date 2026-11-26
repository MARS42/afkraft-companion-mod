package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawContainer;
import com.mars4two.ikang.core.snapshots.model.RawContainerType;
import com.mars4two.ikang.core.util.ContainerView;

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
        return SnapshotType.CONTAINER.snapshotName();
    }

    @Override
    public String key() {
        return conflationKey;
    }

    @Override
    public Object changeToken() {
        return List.of(data.blockPos(), data.items(), data.deleted());
    }

    public static ContainerSnapshot of(Set<UUID> playerIds, ContainerView containerView) {
        return new ContainerSnapshot(
                playerIds,
                new RawContainer(containerView.type(),
                        containerView.blockCoord(),
                        containerView.containerName(),
                        containerView.items(),
                        false)
        );
    }

    public static ContainerSnapshot ofDeleted(Set<UUID> playerIds, ContainerView containerView) {
        return new ContainerSnapshot(
                playerIds,
                new RawContainer(containerView.type(),
                        containerView.blockCoord(),
                        containerView.containerName(),
                        containerView.items(),
                        true)
        );
    }

    public static ContainerSnapshot ofPortable(Set<UUID> playerIds, UUID containerId, ContainerView containerView) {
        return new ContainerSnapshot(
                playerIds,
                new RawContainer(
                        RawContainerType.portableContainer(containerView.blockId(), containerId),
                        containerView.blockCoord(),
                        containerView.containerName(),
                        containerView.items(),
                        false
                ),
                "portable:" + containerId
        );
    }

    public static ContainerSnapshot ofPortableDeleted(Set<UUID> playerIds, UUID containerId, ContainerView containerView) {
        return new ContainerSnapshot(
                playerIds,
                new RawContainer(
                        RawContainerType.portableContainer(containerView.blockId(), containerId),
                        containerView.blockCoord(),
                        containerView.containerName(),
                        containerView.items(),
                        true
                ),
                "portable:" + containerId
        );
    }
}
