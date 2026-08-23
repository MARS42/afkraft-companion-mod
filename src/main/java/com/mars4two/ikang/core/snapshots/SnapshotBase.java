package com.mars4two.ikang.core.snapshots;

public interface SnapshotBase {
    String getSnapshotName();
    String key();
    String changeToken();
}
