package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public interface BlockEntityWrapper {
    String afkraft_companion$type();

    default BlockPos afkraft_companion$blockPos() {
        if(!(this instanceof BlockEntity blockEntity))
            return null;
        return blockEntity.getBlockPos();
    }

    default String afkraft_companion$containerName() {
        if(!(this instanceof BlockEntity blockEntity))
            return null;

        var key = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());

        if(!(this instanceof Nameable nameable))
            return key.toString();

        return nameable.hasCustomName() ? nameable.getPlainTextName() : key.toString();
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
