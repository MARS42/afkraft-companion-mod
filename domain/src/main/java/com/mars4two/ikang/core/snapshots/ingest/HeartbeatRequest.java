package com.mars4two.ikang.core.snapshots.ingest;

import java.util.UUID;

public record HeartbeatRequest(UUID serverId, UUID profileId) {
}
