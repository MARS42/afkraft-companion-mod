package com.mars4two.ikang.core.snapshots.model;

import net.minecraft.core.BlockPos;

import java.util.List;

public record RawContainer(String type,
                           BlockPos blockPos,
                           String containerName,
                           List<RawItemStack> items) implements RawObject {
    @Override
    public Object[] payloadValues() {
        return new Object[]{
                type,
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ(),
                containerName,
                items
        };
    }
}
