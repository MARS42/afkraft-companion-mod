package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawContainer;
import com.mars4two.ikang.core.util.ContainerView;

import java.util.List;
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
        return List.of(data.blockPos(), data.items(), data.deleted());
    }

    public static LoadStationSnapshot of(Set<UUID> playerIds, ContainerView containerView) {
        return new LoadStationSnapshot(
                playerIds,
                new RawContainer(containerView.type(),
                        containerView.blockCoord(),
                        containerView.containerName(),
                        containerView.items(),
                        false)
        );
    }

    public static LoadStationSnapshot ofDeleted(Set<UUID> playerIds, ContainerView containerView) {
        return new LoadStationSnapshot(
                playerIds,
                new RawContainer(containerView.type(),
                        containerView.blockCoord(),
                        containerView.containerName(),
                        containerView.items(),
                        true)
        );
    }
}
