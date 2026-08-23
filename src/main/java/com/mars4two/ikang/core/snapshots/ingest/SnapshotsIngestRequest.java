package com.mars4two.ikang.core.snapshots.ingest;

import com.mars4two.ikang.core.snapshots.SnapshotBase;

import java.util.List;

public record SnapshotsIngestRequest(List<SnapshotContent> snapshots) {
    public record SnapshotContent(String name, Object data) {
        public static <T extends SnapshotBase>SnapshotContent fromBase(T snapshotBase) {
            return new SnapshotContent(snapshotBase.getSnapshotName(), snapshotBase);
        }
    }
}
