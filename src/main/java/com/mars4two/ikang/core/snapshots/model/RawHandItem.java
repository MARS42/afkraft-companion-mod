package com.mars4two.ikang.core.snapshots.model;

import java.util.List;

/**
 * Immutable raw data representing an item held in a hand.
 */
public record RawHandItem(
        String itemName,
        int count,
        List<String> enchantments
) {
    public String toPayloadString() {
        String ench = (enchantments == null || enchantments.isEmpty()) ? "" : String.join(", ", enchantments);
        return "i=" + itemName +
                "|c=" + count +
                "|e=" + ench;
    }
}
