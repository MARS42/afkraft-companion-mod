package com.mars4two.ikang.core.model;

import java.util.Objects;

/**
 * Pure domain identifier for a world dimension (e.g. {@code minecraft:overworld}).
 */
public record DimensionKey(String value) {

    public DimensionKey {
        Objects.requireNonNull(value, "value");
    }

    public static DimensionKey of(String namespace, String path) {
        return new DimensionKey(namespace + ":" + path);
    }

    @Override
    public String toString() {
        return value;
    }
}
