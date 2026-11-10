package com.mars4two.ikang.core.snapshots.model;

public record RawItemStackExtraData(int currentDamage, int maxDamage, int remainingUsages) implements RawObject {

    public RawItemStackExtraData(int currentDamage, int maxDamage) {
        this(currentDamage, maxDamage, Math.max(0, maxDamage - currentDamage));
    }

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                currentDamage,
                maxDamage,
                remainingUsages
        };
    }
}
