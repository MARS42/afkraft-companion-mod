package com.mars4two.ikang.core.snapshots.model;

public record RawContainerExtraData(Object... payload) implements RawObject {

    public static final RawContainerExtraData EMPTY = new RawContainerExtraData();

    @Override
    public Object[] payloadValues() {
        return payload;
    }
}
