package com.mars4two.ikang.core.snapshots.model;

import com.mars4two.ikang.core.model.BlockCoord;

import java.util.List;

public record RawContainer(RawContainerType typeData,
                           BlockCoord blockPos,
                           String containerName,
                           List<RawItemStack> items,
                           boolean deleted) implements RawObject {
    @Override
    public Object[] payloadValues() {
        return new Object[]{
                typeData,
                blockPos.x(),
                blockPos.y(),
                blockPos.z(),
                containerName,
                items,
                deleted
        };
    }
}
