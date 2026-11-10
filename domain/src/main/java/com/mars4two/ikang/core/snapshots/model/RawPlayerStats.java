package com.mars4two.ikang.core.snapshots.model;

/**
 * Immutable raw snapshot of player status without string formatting.
 */
public record RawPlayerStats(
        String gameMode,
        float health,
        float maxHealth,
        int foodLevel,
        int oxygenLevel,
        int expLevel,
        float expProgress,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) implements RawObject {

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                gameMode,
                health,
                maxHealth,
                foodLevel,
                oxygenLevel,
                expLevel,
                expProgress,
                x,
                y,
                z,
                yaw,
                pitch
        };
    }
}
