package com.mars4two.ikang.platform.mc;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * Isolates {@link CompoundTag} and {@link DataComponents#CUSTOM_DATA} return-type differences
 * across Minecraft versions (e.g. {@code Optional<String>} in 1.21.5+/26.2 vs {@code String} in 1.21.1).
 */
public final class NbtCompatAdapter {

    private NbtCompatAdapter() {
    }

    public static Optional<String> getString(CompoundTag tag, String key) {
        if (tag == null || key == null) {
            return Optional.empty();
        }
        Optional<String> raw = tag.getString(key);
        if (raw.isEmpty() || raw.get().isBlank()) {
            return Optional.empty();
        }
        return raw;
    }

    public static Optional<String> getBlockEntityCustomString(BlockEntity blockEntity, String key) {
        if (blockEntity == null) {
            return Optional.empty();
        }
        CustomData customData = blockEntity.components().get(DataComponents.CUSTOM_DATA);
        return extractCustomString(customData, key);
    }

    public static Optional<String> getItemStackCustomString(ItemStack itemStack, String key) {
        if (itemStack == null || itemStack.isEmpty()) {
            return Optional.empty();
        }
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        return extractCustomString(customData, key);
    }

    public static void setBlockEntityCustomString(BlockEntity blockEntity, String key, String value) {
        CustomData currentCustomData = blockEntity.components()
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CustomData updatedCustomData = currentCustomData.update(tag -> tag.putString(key, value));

        DataComponentMap updatedComponents = DataComponentMap.builder()
                .addAll(blockEntity.components())
                .set(DataComponents.CUSTOM_DATA, updatedCustomData)
                .build();
        blockEntity.setComponents(updatedComponents);
        blockEntity.setChanged();
    }

    public static void removeBlockEntityCustomKey(BlockEntity blockEntity, String key) {
        if (blockEntity == null) {
            return;
        }
        CustomData currentCustomData = blockEntity.components().get(DataComponents.CUSTOM_DATA);
        if (currentCustomData == null || currentCustomData.isEmpty()) {
            return;
        }

        CompoundTag tag = currentCustomData.copyTag();
        if (!tag.contains(key)) {
            return;
        }
        tag.remove(key);

        DataComponentMap baseWithoutCustomData = blockEntity.components()
                .filter(type -> type != DataComponents.CUSTOM_DATA);

        if (tag.isEmpty()) {
            blockEntity.setComponents(baseWithoutCustomData);
        } else {
            DataComponentMap updatedComponents = DataComponentMap.builder()
                    .addAll(baseWithoutCustomData)
                    .set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
                    .build();
            blockEntity.setComponents(updatedComponents);
        }
        blockEntity.setChanged();
    }

    public static void setItemStackCustomString(ItemStack stack, String key, String value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(key, value));
    }

    private static Optional<String> extractCustomString(CustomData customData, String key) {
        if (customData == null || customData.isEmpty()) {
            return Optional.empty();
        }
        return getString(customData.copyTag(), key);
    }
}
