package com.mars4two.ikang.core.snapshots.model;

import java.util.List;

/**
 * Root container for the player's mobcap state, creature breakdown by type,
 * and 3D spawn sphere geometry/clusters.
 */
public record RawMobSphere(
        RawSpawnGeometry geometry,
        List<RawCategoryCap> categories,
        List<RawMobTypeBreakdown> breakdown,
        List<RawMobCluster> clusters
) implements RawObject {

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                geometry,
                categories,
                breakdown,
                clusters
        };
    }
}
