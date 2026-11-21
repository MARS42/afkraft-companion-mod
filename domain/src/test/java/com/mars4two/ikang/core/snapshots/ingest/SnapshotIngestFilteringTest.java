package com.mars4two.ikang.core.snapshots.ingest;

import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.preferences.SnapshotPreferencesManager;
import com.mars4two.ikang.core.preferences.WorldPreferencesService;
import com.mars4two.ikang.core.snapshots.SnapshotBase;
import com.mars4two.ikang.core.snapshots.SnapshotType;
import com.mars4two.ikang.core.snapshots.model.*;
import com.mars4two.ikang.core.util.Conflater;
import com.mars4two.ikang.core.util.ContainerView;
import com.mars4two.ikang.core.util.InventoryView;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotIngestFilteringTest {

    @Test
    void disabledSnapshotsSkipLazyExtractionCompletely() throws Exception {
        SnapshotPreferencesManager prefs = new SnapshotPreferencesManager(new WorldPreferencesService(), Optional::empty);
        SnapshotIngestManager ingest = new SnapshotIngestManager(prefs);
        List<SnapshotBase> emitted = new ArrayList<>();
        try (Conflater<SnapshotBase> conflater = new Conflater<>(
                60_000,
                SnapshotBase::key,
                SnapshotBase::changeToken,
                SnapshotBase::getSnapshotName,
                emitted::addAll,
                null,
                0,
                0,
                null
        )) {
            ingest.attachConflaterForTest(conflater);

            prefs.applyLocalPreferencesForTest(Map.of(
                    SnapshotType.PLAYER_MOB_SPHERE.snapshotName(), false,
                    SnapshotType.PLAYER_STATS.snapshotName(), false,
                    SnapshotType.CONTAINER.snapshotName(), false,
                    SnapshotType.PLAYER_INVENTORY.snapshotName(), false
            ));

            UUID playerId = UUID.randomUUID();
            AtomicInteger mobSphereCalls = new AtomicInteger();
            AtomicInteger statsCalls = new AtomicInteger();
            AtomicInteger effectsCalls = new AtomicInteger();
            AtomicInteger containerItemCalls = new AtomicInteger();
            AtomicInteger inventoryItemCalls = new AtomicInteger();
            AtomicInteger hotbarItemCalls = new AtomicInteger();

            PlayerStatsContainer statsView = new PlayerStatsContainer() {
                @Override
                public UUID profileId() {
                    return playerId;
                }

                @Override
                public RawPlayerStats stats() {
                    statsCalls.incrementAndGet();
                    return new RawPlayerStats("survival", 20f, 20f, 20, 300, 0, 0f, 0, 64, 0, 0f, 0f);
                }

                @Override
                public RawPlayerEffects effects() {
                    effectsCalls.incrementAndGet();
                    return new RawPlayerEffects(List.of());
                }

                @Override
                public RawPlayerHands handsStats() {
                    return new RawPlayerHands(RawItemStack.EMPTY, RawItemStack.EMPTY);
                }

                @Override
                public RawPlayerAttackCadence attackCadence() {
                    return RawPlayerAttackCadence.idle(12.5f);
                }

                @Override
                public RawMobSphere mobSphere() {
                    mobSphereCalls.incrementAndGet();
                    return new RawMobSphere(
                            new RawSpawnGeometry("minecraft:overworld", 0, 64, 0, -64, 320, 289, 0, 64, 0),
                            List.of(),
                            List.of(),
                            List.of()
                    );
                }
            };

            ContainerView containerView = new ContainerView() {
                @Override
                public RawContainerType type() {
                    return RawContainerType.singleChest();
                }

                @Override
                public Optional<UUID> containerId() {
                    return Optional.empty();
                }

                @Override
                public BlockCoord blockCoord() {
                    return new BlockCoord(1, 64, 1);
                }

                @Override
                public String blockId() {
                    return "minecraft:chest";
                }

                @Override
                public String containerName() {
                    return "Chest";
                }

                @Override
                public List<RawItemStack> items() {
                    containerItemCalls.incrementAndGet();
                    return List.of();
                }
            };

            InventoryView inventoryView = new InventoryView() {
                @Override
                public UUID playerId() {
                    return playerId;
                }

                @Override
                public List<RawItemStack> hotbarItems() {
                    hotbarItemCalls.incrementAndGet();
                    return List.of();
                }

                @Override
                public List<RawItemStack> inventoryItems() {
                    inventoryItemCalls.incrementAndGet();
                    return List.of();
                }

                @Override
                public List<RawItemStack> playerEquipment() {
                    return List.of();
                }

                @Override
                public int armorValue() {
                    return 0;
                }

                @Override
                public int selectedSlot() {
                    return 0;
                }
            };

            ingest.ingestPlayerMobSphere(statsView);
            ingest.ingestPlayerStats(statsView);
            ingest.ingestContainerBlockEntity(Set.of(playerId), containerView);
            ingest.ingestPlayerFullInventoryState(inventoryView);
            conflater.flush();

            assertEquals(0, mobSphereCalls.get(), "mobSphere() must not run when PLAYER_MOB_SPHERE is disabled");
            assertEquals(0, statsCalls.get(), "stats() must not run when PLAYER_STATS is disabled");
            assertEquals(0, containerItemCalls.get(), "container items() must not run when CONTAINER is disabled");
            assertEquals(0, inventoryItemCalls.get(), "inventoryItems() must not run when PLAYER_INVENTORY is disabled");
            assertEquals(1, effectsCalls.get(), "enabled effects snapshot must still extract");
            assertEquals(1, hotbarItemCalls.get(), "enabled hotbar snapshot must still extract");

            List<String> emittedNames = emitted.stream().map(SnapshotBase::getSnapshotName).toList();
            assertTrue(emittedNames.contains(SnapshotType.PLAYER_EFFECT_STATS.snapshotName()));
            assertTrue(emittedNames.contains(SnapshotType.PLAYER_HOTBAR.snapshotName()));
            assertTrue(emittedNames.stream().noneMatch(n ->
                    n.equals(SnapshotType.PLAYER_MOB_SPHERE.snapshotName())
                            || n.equals(SnapshotType.PLAYER_STATS.snapshotName())
                            || n.equals(SnapshotType.CONTAINER.snapshotName())
                            || n.equals(SnapshotType.PLAYER_INVENTORY.snapshotName())
            ));
        }
    }

    @Test
    void disableEvictsPendingAndReenableReemitsIdenticalUnchangedData() throws Exception {
        SnapshotPreferencesManager prefs = new SnapshotPreferencesManager(new WorldPreferencesService(), Optional::empty);
        SnapshotIngestManager ingest = new SnapshotIngestManager(prefs);
        List<SnapshotBase> emitted = new ArrayList<>();
        try (Conflater<SnapshotBase> conflater = new Conflater<>(
                60_000,
                SnapshotBase::key,
                SnapshotBase::changeToken,
                SnapshotBase::getSnapshotName,
                emitted::addAll,
                null,
                0,
                0,
                null
        )) {
            ingest.attachConflaterForTest(conflater);

            UUID playerId = UUID.randomUUID();
            PlayerStatsContainer statsView = new PlayerStatsContainer() {
                @Override
                public UUID profileId() {
                    return playerId;
                }

                @Override
                public RawPlayerStats stats() {
                    return new RawPlayerStats("survival", 20f, 20f, 20, 300, 5, 0.5f, 10, 64, 10, 0f, 0f);
                }

                @Override
                public RawPlayerEffects effects() {
                    return new RawPlayerEffects(List.of());
                }

                @Override
                public RawPlayerHands handsStats() {
                    return new RawPlayerHands(RawItemStack.EMPTY, RawItemStack.EMPTY);
                }

                @Override
                public RawPlayerAttackCadence attackCadence() {
                    return RawPlayerAttackCadence.idle(12.5f);
                }

                @Override
                public RawMobSphere mobSphere() {
                    return new RawMobSphere(
                            new RawSpawnGeometry("minecraft:overworld", 0, 64, 0, -64, 320, 289, 0, 64, 0),
                            List.of(),
                            List.of(),
                            List.of()
                    );
                }
            };

            // 1. Emit initial playerMobSphere
            ingest.ingestPlayerMobSphere(statsView);
            conflater.flush();
            assertEquals(1, emitted.size());

            // 2. Identical state while enabled is conflated (0 new emissions)
            ingest.ingestPlayerMobSphere(statsView);
            conflater.flush();
            assertEquals(1, emitted.size());

            // 3. Disable -> Re-enable invalidates conflater group
            prefs.applyLocalPreferencesForTest(Map.of(SnapshotType.PLAYER_MOB_SPHERE.snapshotName(), false));
            prefs.applyLocalPreferencesForTest(Map.of(SnapshotType.PLAYER_MOB_SPHERE.snapshotName(), true));

            // 4. Same unchanged state MUST emit again immediately
            ingest.ingestPlayerMobSphere(statsView);
            conflater.flush();
            assertEquals(2, emitted.size());

            // 5. Submit while enabled, then disable BEFORE flush -> pending entry must be evicted
            prefs.applyLocalPreferencesForTest(Map.of(SnapshotType.PLAYER_STATS.snapshotName(), true));
            ingest.ingestPlayerStats(statsView);
            prefs.applyLocalPreferencesForTest(Map.of(SnapshotType.PLAYER_STATS.snapshotName(), false));
            emitted.clear();
            conflater.flush();
            assertTrue(emitted.stream().noneMatch(s -> s.getSnapshotName().equals(SnapshotType.PLAYER_STATS.snapshotName())));
        }
    }
}
