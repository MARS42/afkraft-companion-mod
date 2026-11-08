package com.mars4two.ikang.core.snapshots.model;

import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.util.MinecraftIdsUtils;

import java.util.Map;
import java.util.UUID;

public record RawContainerType(String type, RawContainerExtraData extraData, RawContainerLayout layout) implements RawObject {

    public static RawContainerType ofType(String type) {
        return new RawContainerType(type, RawContainerExtraData.EMPTY, RawContainerGridLayout.DEFAULT);
    }

    public static RawContainerType portableContainer(String type, UUID containerId) {
        return new RawContainerType(type,
                new RawContainerExtraData(Map.of("containerId", containerId.toString())),
                RawContainerGridLayout.DEFAULT);
    }

    public static RawContainerType singleChest() {
        return new RawContainerType(MinecraftIdsUtils.blockAlias("minecraft:chest"), RawContainerExtraData.EMPTY, RawContainerGridLayout.DEFAULT);
    }

    public static RawContainerType doubleChest(BlockCoord first, BlockCoord last) {
        return new RawContainerType(MinecraftIdsUtils.blockAlias("minecraft:chest"),
                new RawContainerExtraData(Map.of("bicombiner", Map.of("first", first, "last", last))),
                RawContainerGridLayout.DEFAULT);
    }

    @Override
    public Object[] payloadValues() {
        return new Object[] {
                MinecraftIdsUtils.blockAlias(type),
                extraData,
                layout
        };
    }
}
