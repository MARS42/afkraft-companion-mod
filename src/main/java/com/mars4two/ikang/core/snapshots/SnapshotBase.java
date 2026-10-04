package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawObject;

import java.util.Set;
import java.util.UUID;

public interface SnapshotBase {
    String getSnapshotName();
    Set<UUID> playerIds();
    <T extends RawObject> T data();
    String key();

    default Object changeToken(){
        return data();
    }
}
