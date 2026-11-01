package com.mars4two.ikang.core.util;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import java.util.Optional;
import java.util.UUID;

public final class PortableContainerIdentityHelper {

    public static final String CONTAINER_ID_TAG = "afkraft_container_id";

    private PortableContainerIdentityHelper() {
        // Utility class, no instances allowed
    }

    public static boolean isPortableContainer(BlockEntity blockEntity) {
        return blockEntity instanceof ShulkerBoxBlockEntity;
    }

    public static Optional<UUID> getContainerId(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return Optional.empty();
        }
        CustomData customData = blockEntity.components().get(DataComponents.CUSTOM_DATA);
        return parseContainerId(customData);
    }

    public static Optional<UUID> getContainerId(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return Optional.empty();
        }
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        return parseContainerId(customData);
    }

    public static UUID getOrCreateContainerId(BlockEntity blockEntity) {
        Optional<UUID> existing = getContainerId(blockEntity);
        if (existing.isPresent()) {
            return existing.get();
        }

        UUID generated = UUID.randomUUID();
        CustomData currentCustomData = blockEntity.components().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CustomData updatedCustomData = currentCustomData.update(tag -> tag.putString(CONTAINER_ID_TAG, generated.toString()));

        DataComponentMap updatedComponents = DataComponentMap.builder()
                .addAll(blockEntity.components())
                .set(DataComponents.CUSTOM_DATA, updatedCustomData)
                .build();
        blockEntity.setComponents(updatedComponents);
        blockEntity.setChanged();
        return generated;
    }

    public static void removeContainerId(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return;
        }
        CustomData currentCustomData = blockEntity.components().get(DataComponents.CUSTOM_DATA);
        if (currentCustomData == null || currentCustomData.isEmpty()) {
            return;
        }

        CompoundTag tag = currentCustomData.copyTag();
        if (!tag.contains(CONTAINER_ID_TAG)) {
            return;
        }
        tag.remove(CONTAINER_ID_TAG);

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

    public static void copyContainerIdToStack(BlockEntity blockEntity, ItemStack stack) {
        if (blockEntity == null || stack == null || stack.isEmpty()) {
            return;
        }
        getContainerId(blockEntity).ifPresent(containerId ->
                CustomData.update(DataComponents.CUSTOM_DATA, stack,
                        tag -> tag.putString(CONTAINER_ID_TAG, containerId.toString()))
        );
    }

    private static Optional<UUID> parseContainerId(CustomData customData) {
        if (customData == null || customData.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag tag = customData.copyTag();
        Optional<String> rawId = tag.getString(CONTAINER_ID_TAG);
        if (rawId.isEmpty() || rawId.get().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(rawId.get()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
