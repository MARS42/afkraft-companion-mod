package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.snapshots.model.RawContainerType;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.platform.mc.InventoryEquipmentAdapter;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockEntityWrapper {
    RawContainerType afkraft_companion$type();

    ContainerView afkraft_companion$asContainerView();

    default Optional<UUID> afkraft_companion$containerId() {
        if (!(this instanceof BlockEntity blockEntity)) {
            return Optional.empty();
        }
        return PortableContainerIdentityHelper.getContainerId(blockEntity);
    }

    default BlockPos afkraft_companion$blockPos() {
        if (!(this instanceof BlockEntity blockEntity)) {
            return null;
        }
        return blockEntity.getBlockPos();
    }

    default BlockCoord afkraft_companion$blockCoord() {
        BlockPos pos = afkraft_companion$blockPos();
        return pos != null ? new BlockCoord(pos.getX(), pos.getY(), pos.getZ()) : null;
    }

    default String afkraft_companion$blockId() {
        if (!(this instanceof BlockEntity blockEntity)) {
            return null;
        }
        return WorldAndEntityAdapter.blockId(blockEntity.getBlockState().getBlock());
    }

    default String afkraft_companion$containerName() {
        if (!(this instanceof Nameable nameable)) {
            return null;
        }
        return InventoryEquipmentAdapter.getCustomContainerName(nameable);
    }

    default List<RawItemStack> afkraft_companion$getItems() {
        if (!(this instanceof BaseContainerBlockEntity container)) {
            return List.of();
        }

        var iterator = container.iterator();
        List<RawItemStack> items = new ArrayList<>();

        int index = 0;
        while (iterator.hasNext()) {
            var itemStack = iterator.next();
            if (!itemStack.isEmpty()) {
                items.add(ItemStackMapper.from(index, itemStack));
            }
            index++;
        }
        return items;
    }
}
