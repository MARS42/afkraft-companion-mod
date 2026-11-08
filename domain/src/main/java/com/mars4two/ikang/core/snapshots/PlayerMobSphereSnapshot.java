package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawCategoryCap;
import com.mars4two.ikang.core.snapshots.model.RawMobCluster;
import com.mars4two.ikang.core.snapshots.model.RawMobSphere;
import com.mars4two.ikang.core.snapshots.model.RawMobTypeBreakdown;
import com.mars4two.ikang.core.util.PlayerStatsContainer;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record PlayerMobSphereSnapshot(Set<UUID> playerIds, RawMobSphere data) implements SnapshotBase {

    /**
     * Quantized token so sub-block player movement does not trigger redundant emissions
     * when counts, breakdown, and 16x16x16 section clusters have not changed.
     */
    private record QuantizedMobSphereToken(
            String dimension,
            int blockX,
            int blockY,
            int blockZ,
            int spawnableChunkCount,
            List<RawCategoryCap> categories,
            List<RawMobTypeBreakdown> breakdown,
            List<RawMobCluster> clusters
    ) {
    }

    @Override
    public String getSnapshotName() {
        return "playerMobSphere";
    }

    @Override
    public String key() {
        return playerIds.stream().map(UUID::toString).collect(Collectors.joining()) + getSnapshotName();
    }

    @Override
    public Object changeToken() {
        var g = data.geometry();
        return new QuantizedMobSphereToken(
                g.dimension(),
                (int) Math.floor(g.centerX()),
                (int) Math.floor(g.centerY()),
                (int) Math.floor(g.centerZ()),
                g.spawnableChunkCount(),
                data.categories(),
                data.breakdown(),
                data.clusters()
        );
    }

    public static PlayerMobSphereSnapshot of(PlayerStatsContainer player) {
        return new PlayerMobSphereSnapshot(
                Set.of(player.profileId()),
                player.mobSphere()
        );
    }
}
