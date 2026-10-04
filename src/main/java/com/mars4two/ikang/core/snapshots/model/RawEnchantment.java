package com.mars4two.ikang.core.snapshots.model;

public record RawEnchantment(String id, int level) implements RawObject {

    @Override
    public Object[] payloadValues() {
        return new Object[]{id, level};
    }
}
