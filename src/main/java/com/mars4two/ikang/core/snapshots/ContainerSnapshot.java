package com.mars4two.ikang.core.snapshots;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

import java.util.Set;
import java.util.UUID;


public record ContainerSnapshot(Set<UUID> playerIds, BlockPos blockPos, String containerName, String data) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "container";
    }

    @Override
    public String key() {
        return blockPos.toString();
    }

    @Override
    public String changeToken() {
        return data;
    }

    public static ContainerSnapshot of(Set<UUID> playerIds, BaseContainerBlockEntity containerBlockEntity) {

        var iterator = containerBlockEntity.iterator();
        var content = new StringBuilder("[");

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

            content.append("\"");
            content.append("n=").append(new JsonPrimitive(itemStack.getDisplayName().getString()));
            content.append("|c=").append(itemStack.count());
            content.append("|i=").append(index);
            content.append("\"");
            index++;
        }

        content.append("]");
        return new ContainerSnapshot(
                playerIds,
                containerBlockEntity.getBlockPos(),
                containerBlockEntity.getPlainTextName(),
                content.toString());
    }
}
