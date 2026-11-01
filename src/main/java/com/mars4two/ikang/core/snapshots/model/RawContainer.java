package com.mars4two.ikang.core.snapshots.model;

import net.minecraft.core.BlockPos;

import java.util.List;

public record RawContainer(RawContainerType typeData,
                           BlockPos blockPos,
                           String containerName,
                           List<RawItemStack> items,
                           boolean deleted) implements RawObject {
    @Override
    public Object[] payloadValues() {
        return new Object[]{
                typeData,
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ(),
                containerName,
                items,
                deleted
        };
    }
}
