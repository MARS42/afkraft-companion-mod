package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawContainerType;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockEntityWrapper {
    RawContainerType afkraft_companion$type();

    default Optional<UUID> afkraft_companion$containerId() {
        if (!(this instanceof BlockEntity blockEntity)) {
            return Optional.empty();
        }
        return PortableContainerIdentityHelper.getContainerId(blockEntity);
    }

    default BlockPos afkraft_companion$blockPos() {
        if(!(this instanceof BlockEntity blockEntity))
            return null;
        return blockEntity.getBlockPos();
    }

    default String afkraft_companion$blockId() {
        if(!(this instanceof BlockEntity blockEntity))
            return null;
        return BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock()).toString();
    }

    default String afkraft_companion$containerName() {
        if(!(this instanceof Nameable nameable))
            return null;

        return nameable.hasCustomName() ? nameable.getPlainTextName() : null;
    }

    default List<RawItemStack> afkraft_companion$getItems(){
        if (!(this instanceof BaseContainerBlockEntity container)) return List.of();

        var iterator = container.iterator();
        List<RawItemStack> items = new ArrayList<>();

        int index = 0;
        while (iterator.hasNext()) {
            var itemStack = iterator.next();
            if (!itemStack.isEmpty()) {
                items.add(RawItemStack.from(index, itemStack));
            }
            index++;
        }
        return items;
    }
}
