package com.mars4two.ikang.core.snapshots.model;

public record RawItemStackExtraData(int currentDamage, int maxDamage) implements RawObject {
    @Override
    public Object[] payloadValues() {

        return new Object[]{
                currentDamage,
                maxDamage
        };
    }
}
