package com.mars4two.ikang.core.util;

import com.mars4two.ikang.platform.mc.NbtCompatAdapter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import java.util.Optional;
import java.util.UUID;

public final class PortableContainerIdentityHelper {

    public static final String CONTAINER_ID_TAG = "afkraft_container_id";

    private PortableContainerIdentityHelper() {
    }

    public static boolean isPortableContainer(BlockEntity blockEntity) {
        return blockEntity instanceof ShulkerBoxBlockEntity;
    }

    public static Optional<UUID> getContainerId(BlockEntity blockEntity) {
        return parseUuid(NbtCompatAdapter.getBlockEntityCustomString(blockEntity, CONTAINER_ID_TAG));
    }

    public static Optional<UUID> getContainerId(ItemStack itemStack) {
        return parseUuid(NbtCompatAdapter.getItemStackCustomString(itemStack, CONTAINER_ID_TAG));
    }

    public static UUID getOrCreateContainerId(BlockEntity blockEntity) {
        Optional<UUID> existing = getContainerId(blockEntity);
        if (existing.isPresent()) {
            return existing.get();
        }

        UUID generated = UUID.randomUUID();
        NbtCompatAdapter.setBlockEntityCustomString(blockEntity, CONTAINER_ID_TAG, generated.toString());
        return generated;
    }

    public static void removeContainerId(BlockEntity blockEntity) {
        NbtCompatAdapter.removeBlockEntityCustomKey(blockEntity, CONTAINER_ID_TAG);
    }

    public static void copyContainerIdToStack(BlockEntity blockEntity, ItemStack stack) {
        if (blockEntity == null || stack == null || stack.isEmpty()) {
            return;
        }
        getContainerId(blockEntity).ifPresent(containerId ->
                NbtCompatAdapter.setItemStackCustomString(stack, CONTAINER_ID_TAG, containerId.toString())
        );
    }

    private static Optional<UUID> parseUuid(Optional<String> rawId) {
        if (rawId.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(rawId.get()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
