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
) implements RawObject {

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                gameMode,
                health,
                maxHealth,
                foodLevel,
                expLevel,
                expProgress,
                x,
                y,
                z
        };
    }
}
