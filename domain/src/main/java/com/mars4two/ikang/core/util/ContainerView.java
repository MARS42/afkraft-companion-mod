package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.snapshots.model.RawContainerType;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure domain read-port over a container instance (fixed chest, barrel, shulker box, load station, etc.).
 */
public interface ContainerView {
    RawContainerType type();
    Optional<UUID> containerId();
    BlockCoord blockCoord();
    String blockId();
    String containerName();
    List<RawItemStack> items();
}
