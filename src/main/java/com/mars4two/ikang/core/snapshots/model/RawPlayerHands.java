package com.mars4two.ikang.core.snapshots.model;

/**
 * Immutable raw data holding the player's main and off hand items.
 */
public record RawPlayerHands(
        RawHandItem mainHand,
        RawHandItem offHand
) {
    public String toPayloadString() {
        String mainStr = mainHand != null ? mainHand.toPayloadString() : "null";
        String offStr = offHand != null ? offHand.toPayloadString() : "null";
        return "[\"main=" + mainStr + "\",\"|off=" + offStr + "\"]";
    }
}
