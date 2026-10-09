package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public interface BlockEntityWrapper {
    String afkraft_companion$type();
    String afkraft_companion$defaultName();

    default BlockPos afkraft_companion$blockPos() {
        if(!(this instanceof BlockEntity blockEntity))
            return null;
        return blockEntity.getBlockPos();
    }

    default String afkraft_companion$containerName() {
        if(!(this instanceof Nameable nameable))
            return afkraft_companion$defaultName();

        return nameable.getPlainTextName();
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
