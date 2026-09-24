package com.mars4two.ikang.core.snapshots.model;

/**
 * Immutable raw effect data for an active mob effect.
 */
public record RawPlayerEffect(
        String descriptionId,
        int amplifier,
        int duration,
        boolean ambient,
        boolean visible
) {
    public String toPayloadString() {
        return "\"e=" + descriptionId +
                "|a=" + amplifier +
                "|d=" + duration +
                "|am=" + ambient +
                "|v=" + visible + "\"";
    }
}
