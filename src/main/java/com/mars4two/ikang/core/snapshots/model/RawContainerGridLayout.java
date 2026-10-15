package com.mars4two.ikang.core.snapshots.model;

import java.util.Map;

public final class RawContainerGridLayout extends RawContainerLayout {

    public static final RawContainerGridLayout DEFAULT = new RawContainerGridLayout(3, 9);

    public RawContainerGridLayout(int rows, int columns) {
        super(Map.of("r", rows, "c", columns));
    }

    @Override
    protected String type() {
        return "grid";
    }
}
