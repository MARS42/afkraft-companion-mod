package com.mars4two.ikang.core.snapshots.model;

/**
 * Immutable raw data holding the player's main and off hand items.
 */
public record RawPlayerHands(
        RawItemStack mainHand,
        RawItemStack offHand
) {
    public String toPayloadString() {
        String mainStr = mainHand != null ? mainHand.toPayloadString() : "";
        String offStr = offHand != null ? offHand.toPayloadString() : "";
        return "[\"" + mainStr + "\",\"" + offStr + "\"]";
    }
}
