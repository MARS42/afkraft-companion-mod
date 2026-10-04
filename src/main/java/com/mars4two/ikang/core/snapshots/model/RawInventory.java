package com.mars4two.ikang.core.snapshots.model;

import java.util.List;

public record RawInventory(List<RawItemStack> items) implements RawObject {
    @Override
    public Object[] payloadValues() {
        return new Object[]{items};
    }
}
