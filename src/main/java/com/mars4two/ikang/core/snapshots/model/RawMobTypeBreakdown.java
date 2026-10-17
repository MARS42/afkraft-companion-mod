package com.mars4two.ikang.core.snapshots.model;

/**
 * Breakdown of creatures by EntityType within the player's spawning range,
 * distinguishing cap-counted mobs from persistent mobs and their sphere zones.
 */
public record RawMobTypeBreakdown(
        String entityType,
        String category,
        int capCount,
        int persistentCount,
        int inImmediateZone,
        int inDespawnZone
) implements RawObject {

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                entityType,
                category,
                capCount,
                persistentCount,
                inImmediateZone,
                inDespawnZone
        };
    }
}
