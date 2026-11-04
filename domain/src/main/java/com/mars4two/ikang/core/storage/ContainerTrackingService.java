package com.mars4two.ikang.core.storage;

import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.model.DimensionKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Pure domain state and inverted-index manager for tracked fixed containers, load stations,
 * and portable containers (e.g., Shulker Boxes).
 */
public final class ContainerTrackingService {

    private final Map<UUID, Map<DimensionKey, Map<BlockCoord, ContainerTrackingEntry>>> config = new HashMap<>();
    private final Map<UUID, Set<UUID>> portableContainers = new HashMap<>();

    // Derived inverted indices (not persisted directly; rebuilt on state import)
    private final Map<DimensionKey, Map<BlockCoord, Set<UUID>>> enabledIndex = new HashMap<>();
    private final Map<DimensionKey, Map<BlockCoord, Set<UUID>>> loadStationIndex = new HashMap<>();
    private final Map<UUID, Set<UUID>> portableIndex = new HashMap<>();

    public ContainerTrackingService() {
    }

    public ContainerTrackingService(
            Map<UUID, Map<DimensionKey, Map<BlockCoord, ContainerTrackingEntry>>> positionalState,
            Map<UUID, List<UUID>> portableState
    ) {
        if (positionalState != null) {
            positionalState.forEach((playerId, byDim) -> {
                var dimCopy = new HashMap<DimensionKey, Map<BlockCoord, ContainerTrackingEntry>>();
                byDim.forEach((dimKey, byPos) -> dimCopy.put(dimKey, new HashMap<>(byPos)));
                this.config.put(playerId, dimCopy);
            });
        }
        if (portableState != null) {
            portableState.forEach((playerId, containerIds) -> {
                if (containerIds != null && !containerIds.isEmpty()) {
                    this.portableContainers.put(playerId, new HashSet<>(containerIds));
                }
            });
        }
        rebuildIndices();
    }

    private void rebuildIndices() {
        enabledIndex.clear();
        loadStationIndex.clear();
        portableIndex.clear();

        config.forEach((playerId, byDim) ->
                byDim.forEach((dimKey, byPos) ->
                        byPos.forEach((pos, entry) -> {
                            if (entry.enabled()) {
                                if (entry.mode() == ContainerTrackingMode.LOAD_STATION) {
                                    indexAdd(loadStationIndex, playerId, dimKey, pos);
                                } else if (entry.mode() == ContainerTrackingMode.CONTAINER) {
                                    indexAdd(enabledIndex, playerId, dimKey, pos);
                                }
                            }
                        })));

        portableContainers.forEach((playerId, containerIds) ->
                containerIds.forEach(containerId -> portableIndexAdd(playerId, containerId)));
    }

    private static void indexAdd(Map<DimensionKey, Map<BlockCoord, Set<UUID>>> targetIndex, UUID playerId, DimensionKey dimKey, BlockCoord pos) {
        targetIndex
                .computeIfAbsent(dimKey, k -> new HashMap<>())
                .computeIfAbsent(pos, k -> new HashSet<>())
                .add(playerId);
    }

    private static void indexRemove(Map<DimensionKey, Map<BlockCoord, Set<UUID>>> targetIndex, UUID playerId, DimensionKey dimKey, BlockCoord pos) {
        var byPos = targetIndex.get(dimKey);
        if (byPos == null) return;
        var owners = byPos.get(pos);
        if (owners == null) return;
        owners.remove(playerId);
        if (owners.isEmpty()) byPos.remove(pos);
        if (byPos.isEmpty()) targetIndex.remove(dimKey);
    }

    private void portableIndexAdd(UUID playerId, UUID containerId) {
        portableIndex.computeIfAbsent(containerId, k -> new HashSet<>()).add(playerId);
    }

    private void portableIndexRemove(UUID playerId, UUID containerId) {
        var owners = portableIndex.get(containerId);
        if (owners == null) return;
        owners.remove(playerId);
        if (owners.isEmpty()) portableIndex.remove(containerId);
    }

    public void set(UUID playerId, DimensionKey dimension, BlockCoord pos, boolean active) {
        setPositionalMode(playerId, dimension, pos, active ? ContainerTrackingMode.CONTAINER : ContainerTrackingMode.OFF);
    }

    public void setPositionalMode(UUID playerId, DimensionKey dimension, BlockCoord pos, ContainerTrackingMode mode) {
        var byPos = this.config.computeIfAbsent(playerId, k -> new HashMap<>())
                .computeIfAbsent(dimension, k -> new HashMap<>());

        indexRemove(enabledIndex, playerId, dimension, pos);
        indexRemove(loadStationIndex, playerId, dimension, pos);

        if (mode == null || mode == ContainerTrackingMode.OFF) {
            byPos.remove(pos);
        } else {
            byPos.put(pos, new ContainerTrackingEntry(true, mode));
            if (mode == ContainerTrackingMode.LOAD_STATION) {
                indexAdd(loadStationIndex, playerId, dimension, pos);
            } else if (mode == ContainerTrackingMode.CONTAINER) {
                indexAdd(enabledIndex, playerId, dimension, pos);
            }
        }
    }

    public void setPortableContainer(UUID playerId, UUID containerId, boolean active) {
        if (active) {
            this.portableContainers.computeIfAbsent(playerId, k -> new HashSet<>()).add(containerId);
            portableIndexAdd(playerId, containerId);
        } else {
            var set = this.portableContainers.get(playerId);
            if (set != null) {
                set.remove(containerId);
                if (set.isEmpty()) {
                    this.portableContainers.remove(playerId);
                }
            }
            portableIndexRemove(playerId, containerId);
        }
    }

    public boolean isPortableContainerEnabled(UUID containerId) {
        var owners = portableIndex.get(containerId);
        return owners != null && !owners.isEmpty();
    }

    public boolean isPortableContainerEnabledFor(UUID playerId, UUID containerId) {
        var owners = portableIndex.get(containerId);
        return owners != null && owners.contains(playerId);
    }

    public Set<UUID> getPortableContainerOwners(UUID containerId) {
        return Set.copyOf(portableIndex.getOrDefault(containerId, Set.of()));
    }

    public ContainerTrackingEntry resolveEffectiveEntry(
            UUID playerId,
            DimensionKey dimension,
            BlockCoord pos,
            Optional<UUID> portableContainerId
    ) {
        if (portableContainerId != null && portableContainerId.isPresent()
                && isPortableContainerEnabledFor(playerId, portableContainerId.get())) {
            return new ContainerTrackingEntry(ContainerTrackingMode.PORTABLE_CONTAINER);
        }
        return getContainerEntry(playerId, dimension, pos)
                .filter(ContainerTrackingEntry::enabled)
                .orElse(ContainerTrackingEntry.OFF);
    }

    public Optional<ContainerTrackingEntry> getContainerEntry(UUID playerId, DimensionKey dimension, BlockCoord pos) {
        var byDim = this.config.get(playerId);
        if (byDim == null) {
            return Optional.empty();
        }
        var byPos = byDim.get(dimension);
        if (byPos == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byPos.get(pos));
    }

    public boolean deleteContainerData(UUID playerId, DimensionKey dimension, BlockCoord pos) {
        var byDim = this.config.get(playerId);
        if (byDim == null) {
            return false;
        }
        var byPos = byDim.get(dimension);
        if (byPos == null) {
            return false;
        }
        byPos.remove(pos);
        indexRemove(enabledIndex, playerId, dimension, pos);
        indexRemove(loadStationIndex, playerId, dimension, pos);
        return true;
    }

    public boolean deleteFixedContainersAt(DimensionKey dimension, BlockCoord pos) {
        var owners = getEnabledOwnersAt(dimension, pos);
        if (owners.isEmpty()) {
            return false;
        }
        for (UUID playerId : owners) {
            var byDim = this.config.get(playerId);
            if (byDim != null) {
                var byPos = byDim.get(dimension);
                if (byPos != null) {
                    var current = byPos.get(pos);
                    if (current == null || current.mode() == ContainerTrackingMode.CONTAINER) {
                        byPos.remove(pos);
                    }
                }
            }
            indexRemove(enabledIndex, playerId, dimension, pos);
        }
        return true;
    }

    public boolean isContainerEnabledIn(UUID playerId, DimensionKey dimension, List<BlockCoord> positions) {
        return positions.stream()
                .anyMatch(pos -> getContainerEntry(playerId, dimension, pos)
                        .map(entry -> entry.enabled() && entry.mode() == ContainerTrackingMode.CONTAINER)
                        .orElse(false));
    }

    public boolean isContainerEnabledAt(DimensionKey dimension, BlockCoord pos) {
        var byPos = enabledIndex.get(dimension);
        if (byPos == null) return false;
        var owners = byPos.get(pos);
        return owners != null && !owners.isEmpty();
    }

    public Set<UUID> getEnabledOwnersAt(DimensionKey dimension, BlockCoord pos) {
        var byPos = enabledIndex.get(dimension);
        if (byPos == null) return Set.of();
        return Set.copyOf(byPos.getOrDefault(pos, Set.of()));
    }

    public boolean isLoadStationEnabledAt(DimensionKey dimension, BlockCoord pos) {
        var byPos = loadStationIndex.get(dimension);
        if (byPos == null) return false;
        var owners = byPos.get(pos);
        return owners != null && !owners.isEmpty();
    }

    public Set<UUID> getLoadStationOwnersAt(DimensionKey dimension, BlockCoord pos) {
        var byPos = loadStationIndex.get(dimension);
        if (byPos == null) return Set.of();
        return Set.copyOf(byPos.getOrDefault(pos, Set.of()));
    }

    public Map<UUID, Map<DimensionKey, Map<BlockCoord, ContainerTrackingEntry>>> exportPositionalState() {
        var copy = new HashMap<UUID, Map<DimensionKey, Map<BlockCoord, ContainerTrackingEntry>>>();
        this.config.forEach((playerId, byDim) -> {
            var dimCopy = new HashMap<DimensionKey, Map<BlockCoord, ContainerTrackingEntry>>();
            byDim.forEach((dimKey, byPos) -> dimCopy.put(dimKey, new HashMap<>(byPos)));
            copy.put(playerId, dimCopy);
        });
        return copy;
    }

    public Map<UUID, List<UUID>> exportPortableContainers() {
        var result = new HashMap<UUID, List<UUID>>();
        this.portableContainers.forEach((playerId, ids) -> {
            if (ids != null && !ids.isEmpty()) {
                result.put(playerId, new ArrayList<>(ids));
            }
        });
        return result;
    }
}
