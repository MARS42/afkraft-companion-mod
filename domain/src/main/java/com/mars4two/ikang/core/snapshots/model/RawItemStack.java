package com.mars4two.ikang.core.snapshots.model;

import com.mars4two.ikang.core.util.MinecraftIdsUtils;

import java.util.List;

public record RawItemStack(
        String id,
        String customName,
        int count,
        List<RawEnchantment> enchantments,
        int index,
        boolean isStackable,
        boolean isDamageable,
        RawItemStackExtraData extraData
) implements RawObject {
    public static final RawItemStack EMPTY = new RawItemStack("", null, 0, List.of(), 0, false, false, null);

    public static RawItemStack of(
            int slot,
            String rawId,
            boolean isBlockItem,
            String customName,
            int count,
            List<RawEnchantment> enchantments,
            boolean isStackable
    ) {
        return of(slot, rawId, isBlockItem, customName, count, enchantments, isStackable, false, null);
    }

    public static RawItemStack of(
            int slot,
            String rawId,
            boolean isBlockItem,
            String customName,
            int count,
            List<RawEnchantment> enchantments,
            boolean isStackable,
            boolean isDamageable,
            RawItemStackExtraData extraData
    ) {
        String aliasedId = isBlockItem
                ? MinecraftIdsUtils.blockAlias(rawId)
                : MinecraftIdsUtils.itemAlias(rawId);
        return new RawItemStack(
                aliasedId,
                customName != null ? customName : "",
                count,
                enchantments != null ? List.copyOf(enchantments) : List.of(),
                slot,
                isStackable,
                isDamageable,
                extraData
        );
    }

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                id,
                customName,
                count,
                enchantments,
                index,
                isStackable,
                isDamageable,
                extraData
        };
    }
}
