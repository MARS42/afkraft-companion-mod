package com.mars4two.ikang.core.snapshots.model;

/**
 * Mobcap state for a single MobCategory (both local player cylinder and dimension global cap).
 */
public record RawCategoryCap(
        String category,
        int localCount,
        int localMax,
        int globalCount,
        int globalMax,
        int despawnRadius
) implements RawObject {

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                category,
                localCount,
                localMax,
                globalCount,
                globalMax,
                despawnRadius
        };
    }
}
