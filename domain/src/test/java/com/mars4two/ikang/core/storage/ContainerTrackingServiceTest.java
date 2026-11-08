package com.mars4two.ikang.core.storage;

import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.model.DimensionKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerTrackingServiceTest {

    private static final DimensionKey OVERWORLD = new DimensionKey("minecraft:overworld");
    private static final UUID PLAYER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAYER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SHULKER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Test
    void tracksPositionalModesMaintainsInvertedIndicesAndPreservesLoadStationsOnBreak() {
        ContainerTrackingService service = new ContainerTrackingService();
        BlockCoord chestPos = new BlockCoord(100, 65, -50);
        BlockCoord stationPos = new BlockCoord(102, 65, -50);

        service.setPositionalMode(PLAYER_A, OVERWORLD, chestPos, ContainerTrackingMode.CONTAINER);
        service.setPositionalMode(PLAYER_B, OVERWORLD, chestPos, ContainerTrackingMode.LOAD_STATION);
        service.setPositionalMode(PLAYER_A, OVERWORLD, stationPos, ContainerTrackingMode.LOAD_STATION);

        assertTrue(service.isContainerEnabledAt(OVERWORLD, chestPos));
        assertEquals(Set.of(PLAYER_A), service.getEnabledOwnersAt(OVERWORLD, chestPos));
        assertTrue(service.isLoadStationEnabledAt(OVERWORLD, chestPos));
        assertEquals(Set.of(PLAYER_B), service.getLoadStationOwnersAt(OVERWORLD, chestPos));
        assertTrue(service.isContainerEnabledIn(PLAYER_A, OVERWORLD, List.of(stationPos, chestPos)));
        assertFalse(service.isContainerEnabledIn(PLAYER_A, OVERWORLD, List.of(stationPos)));

        // Breaking a fixed container at chestPos removes PLAYER_A's CONTAINER entry,
        // but MUST preserve PLAYER_B's LOAD_STATION entry at the same position.
        service.deleteFixedContainersAt(OVERWORLD, chestPos);

        assertFalse(service.isContainerEnabledAt(OVERWORLD, chestPos));
        assertEquals(Optional.empty(), service.getContainerEntry(PLAYER_A, OVERWORLD, chestPos));
        assertTrue(service.isLoadStationEnabledAt(OVERWORLD, chestPos));
        assertEquals(Set.of(PLAYER_B), service.getLoadStationOwnersAt(OVERWORLD, chestPos));
    }

    @Test
    void tracksPortableContainersResolvesEffectiveModeAndRoundTripsSnapshotState() {
        ContainerTrackingService service = new ContainerTrackingService();
        BlockCoord pos = new BlockCoord(0, 80, 0);

        service.setPositionalMode(PLAYER_A, OVERWORLD, pos, ContainerTrackingMode.LOAD_STATION);
        service.setPortableContainer(PLAYER_A, SHULKER_ID, true);

        assertTrue(service.isPortableContainerEnabled(SHULKER_ID));
        assertTrue(service.isPortableContainerEnabledFor(PLAYER_A, SHULKER_ID));
        assertEquals(Set.of(PLAYER_A), service.getPortableContainerOwners(SHULKER_ID));

        // Portable container identity takes precedence for its owner when resolving effective entry
        ContainerTrackingEntry forOwner = service.resolveEffectiveEntry(PLAYER_A, OVERWORLD, pos, Optional.of(SHULKER_ID));
        assertEquals(ContainerTrackingMode.PORTABLE_CONTAINER, forOwner.mode());
        assertTrue(forOwner.enabled());

        // Non-owner falls back to positional or OFF
        ContainerTrackingEntry forOther = service.resolveEffectiveEntry(PLAYER_B, OVERWORLD, pos, Optional.of(SHULKER_ID));
        assertEquals(ContainerTrackingMode.OFF, forOther.mode());
        assertFalse(forOther.enabled());

        // Round-trip state into a fresh service instance rebuilds all inverted indices
        ContainerTrackingService restored = new ContainerTrackingService(
                service.exportPositionalState(),
                service.exportPortableContainers()
        );
        assertTrue(restored.isLoadStationEnabledAt(OVERWORLD, pos));
        assertTrue(restored.isPortableContainerEnabledFor(PLAYER_A, SHULKER_ID));

        restored.setPortableContainer(PLAYER_A, SHULKER_ID, false);
        assertFalse(restored.isPortableContainerEnabled(SHULKER_ID));
    }
}
