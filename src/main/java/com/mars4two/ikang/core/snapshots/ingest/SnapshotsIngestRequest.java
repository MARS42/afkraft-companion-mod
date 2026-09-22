package com.mars4two.ikang.core.snapshots.ingest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SnapshotsIngestRequest(UUID serverId, UUID adminProfileId, Set<UUID> participants, List<SnapshotContent> snapshots) {
    public record SnapshotContent(String name, Set<UUID> playerIds, Object data) {
    }
}
