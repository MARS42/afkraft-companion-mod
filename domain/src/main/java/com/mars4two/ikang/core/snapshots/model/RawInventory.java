package com.mars4two.ikang.core.snapshots.model;

import java.util.List;

public record RawInventory(String type, List<RawItemStack> items) implements RawObject {
    @Override
    public Object[] payloadValues() {
        return new Object[]{type, items};
    }
}
