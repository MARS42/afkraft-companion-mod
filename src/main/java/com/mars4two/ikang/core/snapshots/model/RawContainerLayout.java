package com.mars4two.ikang.core.snapshots.model;

import java.util.Map;

public abstract class RawContainerLayout implements RawObject {

    public final Map<String, Object> payload;

    protected RawContainerLayout(Map<String, Object> payload) {
        this.payload = payload;
    }

    protected abstract String type();

    @Override
    public Object[] payloadValues() {
        return new Object[]{ Map.of(type(), payload) };
    }
}
