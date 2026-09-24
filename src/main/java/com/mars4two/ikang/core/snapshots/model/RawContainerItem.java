package com.mars4two.ikang.core.snapshots.model;

import com.google.gson.JsonPrimitive;

/**
 * Immutable raw data representing a slot item in a container.
 */
public record RawContainerItem(
        String displayName,
        int count,
        int index
) {
    public String toPayloadString() {
        return "\"n=" + new JsonPrimitive(displayName) +
                "|c=" + count +
                "|i=" + index + "\"";
    }
}
