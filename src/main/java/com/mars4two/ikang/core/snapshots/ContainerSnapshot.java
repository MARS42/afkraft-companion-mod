package com.mars4two.ikang.core.snapshots;

import java.util.List;

public record ContainerSnapshot(String containerName, int containerSize, List<ItemContent> content) implements SnapshotBase {
    @Override
    public String getSnapshotName() {
        return containerName;
    }
}
