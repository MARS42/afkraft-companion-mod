package com.mars4two.ikang.core.snapshots.model;

import java.util.List;

public record RawEquipment(List<RawItemStack> equipment) implements RawObject {
    @Override
    public Object[] payloadValues() {
        return new Object[]{
                equipment
        };
    }
}