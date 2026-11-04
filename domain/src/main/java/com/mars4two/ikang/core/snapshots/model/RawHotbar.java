package com.mars4two.ikang.core.snapshots.model;

import java.util.List;

public record RawHotbar(int selectedSlot, List<RawItemStack> items) implements RawObject {
    @Override
    public Object[] payloadValues() {
        return new Object[]{selectedSlot, items};
    }
}
