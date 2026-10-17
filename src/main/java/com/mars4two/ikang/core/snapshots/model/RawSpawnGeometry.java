package com.mars4two.ikang.core.snapshots.model;

/**
 * Spatial geometry context for the player's mob spawning sphere and dimension boundaries.
 */
public record RawSpawnGeometry(
        String dimension,
        double centerX,
        double centerY,
        double centerZ,
        int minY,
        int maxY,
        int spawnableChunkCount,
        Integer worldSpawnX,
        Integer worldSpawnY,
        Integer worldSpawnZ
) implements RawObject {

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                dimension,
                centerX,
                centerY,
                centerZ,
                minY,
                maxY,
                spawnableChunkCount,
                worldSpawnX,
                worldSpawnY,
                worldSpawnZ
        };
    }
}
