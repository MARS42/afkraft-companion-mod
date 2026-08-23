package com.mars4two.ikang.core.snapshots;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;


public record ContainerSnapshot(String containerName, int containerSize, String content, BlockPos pos) implements SnapshotBase {
    @Override
    public String getSnapshotName() {
        return containerName;
    }

    @Override
    public String key() {
        return pos.toString();
    }

    @Override
    public String changeToken() {
        return content;
    }

    public static ContainerSnapshot of(BaseContainerBlockEntity containerBlockEntity) {

        var iterator = containerBlockEntity.iterator();
        var content = new StringBuilder("{\"items\":[");

        int index = 0;
        boolean first = true;
        while (iterator.hasNext()) {

            var itemStack = iterator.next();
            if (itemStack.isEmpty()){
                index++;
                continue;
            }

            if (!first) {
                content.append(",");
            }
            first = false;

            content.append("{");
            content.append("\"n\":").append(new JsonPrimitive(itemStack.getDisplayName().getString())).append(",");
            content.append("\"c\":").append(itemStack.count()).append(",");
            content.append("\"i\":").append(index).append(",");
            content.append("}");
            index++;
        }

        content.append("]}");
        return new ContainerSnapshot(
                containerBlockEntity.getPlainTextName(),
                containerBlockEntity.getContainerSize(),
                content.toString(),
                containerBlockEntity.getBlockPos());
    }
}
