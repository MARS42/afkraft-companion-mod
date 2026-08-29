package com.mars4two.ikang.core.snapshots.ingest;

import java.util.List;

public record SnapshotsIngestRequest(String worldName, List<SnapshotContent> snapshots) {
    public record SnapshotContent(String name, String data) {
    }
}
