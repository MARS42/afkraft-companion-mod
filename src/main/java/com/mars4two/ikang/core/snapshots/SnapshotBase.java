package com.mars4two.ikang.core.snapshots;

import java.util.Set;
import java.util.UUID;

public interface SnapshotBase {
    String getSnapshotName();
    Set<UUID> playerIds();
    String data();
    String key();
    Object changeToken();
}
