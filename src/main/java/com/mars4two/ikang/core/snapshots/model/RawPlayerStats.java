package com.mars4two.ikang.core.snapshots.model;

/**
 * Immutable raw snapshot of player status without string formatting.
 */
public record RawPlayerStats(
        String gameMode,
        float health,
        float maxHealth,
        int foodLevel,
        int expLevel,
        float expProgress,
        double x,
        double y,
        double z
) {
    public String toPayloadString() {
        return "gm=" + (gameMode != null ? gameMode : "") +
                "|h=" + health +
                "|mh=" + maxHealth +
                "|fl=" + foodLevel +
                "|exp=" + expLevel +
                "|expP=" + expProgress +
                "|x=" + x +
                "|y=" + y +
                "|z=" + z;
    }
}
