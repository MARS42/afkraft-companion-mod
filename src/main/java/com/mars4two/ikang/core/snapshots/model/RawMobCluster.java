package com.mars4two.ikang.core.snapshots.model;

/**
 * 3D spatial cluster grouped by 16x16x16 SectionPos for interactive 3D/2D web visualization.
 */
public record RawMobCluster(
        int sectionX,
        int sectionY,
        int sectionZ,
        String category,
        String dominantType,
        int capCount,
        int persistentCount,
        int distanceToPlayer
) implements RawObject {

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                sectionX,
                sectionY,
                sectionZ,
                category,
                dominantType,
                capCount,
                persistentCount,
                distanceToPlayer
        };
    }
}
