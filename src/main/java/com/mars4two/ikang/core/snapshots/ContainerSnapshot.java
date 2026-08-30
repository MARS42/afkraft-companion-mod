package com.mars4two.ikang.core.snapshots;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.UUID;


public record ContainerSnapshot(Set<UUID> playerIds, String containerName, ContainerData data) implements SnapshotBase {
    public record ContainerData(int containerSize, String content, BlockPos pos) {
        @Override
        public @NonNull String toString() {
            return containerSize + ":" + content + ":" + pos.toString();
        }
    }

    @Override
    public String getSnapshotName() {
        return "container";
    }

    @Override
    public String key() {
        return data.pos.toString();
    }

    @Override
    public String changeToken() {
        return data.toString();
    }

    public static ContainerSnapshot of(Set<UUID> playerIds, BaseContainerBlockEntity containerBlockEntity) {

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
                playerIds,
                containerBlockEntity.getPlainTextName(),
                new ContainerData(containerBlockEntity.getContainerSize(),
                        content.toString(),
                        containerBlockEntity.getBlockPos()));
    }
}
