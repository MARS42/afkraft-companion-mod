package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawContainer;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ContainerSnapshot(
        Set<UUID> playerIds,
        RawContainer data
) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "container";
    }

    @Override
    public String key() {
        return data.blockPos().toString();
    }

    public static ContainerSnapshot of(Set<UUID> playerIds, BaseContainerBlockEntity containerBlockEntity) {
        var iterator = containerBlockEntity.iterator();
        List<RawItemStack> items = new ArrayList<>();

        int index = 0;
        while (iterator.hasNext()) {
            var itemStack = iterator.next();
            if (itemStack.isEmpty()) {
                //items.add(RawItemStack.EMPTY);
            }else{
                items.add(RawItemStack.from(index, itemStack));
            }
            index++;
        }

        return new ContainerSnapshot(
                playerIds,
                new RawContainer(containerBlockEntity.getBlockPos(),
                        containerBlockEntity.getPlainTextName(),
                        items)
        );
    }
}
