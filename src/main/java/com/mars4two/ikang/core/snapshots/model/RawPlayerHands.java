package com.mars4two.ikang.core.snapshots.model;

/**
 * Immutable raw data holding the player's main and off hand items.
 */
public record RawPlayerHands(
        RawItemStack mainHand,
        RawItemStack offHand
) implements RawObject{

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                mainHand,
                offHand
        };
    }
}
