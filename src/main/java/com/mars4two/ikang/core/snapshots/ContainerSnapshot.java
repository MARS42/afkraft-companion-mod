package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ContainerSnapshot(
        Set<UUID> playerIds,
        BlockPos blockPos,
        String containerName,
        List<RawItemStack> items
) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "container";
    }

    @Override
    public String key() {
        return blockPos.toString();
    }

    @Override
    public Object changeToken() {
        return items;
    }

    @Override
    public String data() {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        StringBuilder content = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                content.append(",");
            }
            content.append("\"").append(items.get(i).toPayloadString()).append("\"");
        }
        content.append("]");
        return content.toString();
    }

    public static ContainerSnapshot of(Set<UUID> playerIds, BaseContainerBlockEntity containerBlockEntity) {
        var iterator = containerBlockEntity.iterator();
        List<RawItemStack> items = new ArrayList<>();

        int index = 0;
        while (iterator.hasNext()) {
            var itemStack = iterator.next();
            if (itemStack.isEmpty()) {
                items.add(RawItemStack.EMPTY);
            }else{
                items.add(RawItemStack.from(index, itemStack));
            }
            index++;
        }

        return new ContainerSnapshot(
                playerIds,
                containerBlockEntity.getBlockPos(),
                containerBlockEntity.getPlainTextName(),
                items
        );
    }
}
