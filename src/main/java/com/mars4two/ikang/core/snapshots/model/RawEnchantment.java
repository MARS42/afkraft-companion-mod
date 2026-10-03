package com.mars4two.ikang.core.snapshots.model;

public record RawEnchantment(String id, int level) {
    public String toPayloadString() {
        return "i=" + id + "|l=" + level;
    }
}
