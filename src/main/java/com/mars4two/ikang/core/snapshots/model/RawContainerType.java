package com.mars4two.ikang.core.snapshots.model;


import com.mars4two.ikang.core.util.MinecraftIdsUtils;
import net.minecraft.core.BlockPos;

import java.util.Map;

public record RawContainerType(String type, RawContainerExtraData extraData, RawContainerLayout layout) implements RawObject {

    public static RawContainerType ofType(String type) {
        return new RawContainerType(type, RawContainerExtraData.EMPTY, RawContainerGridLayout.DEFAULT);
    }

    public static RawContainerType singleChest() {
        return new RawContainerType(MinecraftIdsUtils.itemAlias("minecraft:chest"), RawContainerExtraData.EMPTY, RawContainerGridLayout.DEFAULT);
    }

    public static RawContainerType doubleChest(BlockPos first, BlockPos last) {
        return new RawContainerType(MinecraftIdsUtils.itemAlias("minecraft:chest"),
                new RawContainerExtraData(Map.of("bicombiner", Map.of("first", first, "last", last))),
                RawContainerGridLayout.DEFAULT);
    }

    @Override
    public Object[] payloadValues() {
        return new Object[] {
                MinecraftIdsUtils.itemAlias(type),
                extraData,
                layout
        };
    }
}
