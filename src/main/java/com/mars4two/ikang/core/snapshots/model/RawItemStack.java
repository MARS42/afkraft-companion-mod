package com.mars4two.ikang.core.snapshots.model;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RawItemStack(
        String id,
        String customName,
        int count,
        List<RawEnchantment> enchantments,
        int index,
        boolean isStackable
) implements RawObject {
    private static final Map<Pattern, String> NAMESPACES_ALIASES = Map.of(
            Pattern.compile("minecraft"), "m"
    );

    private static String aplicarPrimerMatch(String entrada, Map<Pattern, String> reglas) {
        if (entrada == null || reglas == null) {
            return entrada;
        }
        for (Map.Entry<Pattern, String> regla : reglas.entrySet()) {
            Matcher matcher = regla.getKey().matcher(entrada);
            if (matcher.find()) {
                return matcher.replaceAll(regla.getValue());
            }
        }
        return entrada;
    }
    public static RawItemStack EMPTY = new RawItemStack("", null,0, List.of(), 0, false);

    public static RawItemStack from(int slot, ItemStack itemStack) {
        var customName = itemStack.getCustomName() != null ? itemStack.getCustomName().getString() : "";

        var enchantmentHolders = itemStack.getEnchantments().keySet();
        List<RawEnchantment> enchantments = new ArrayList<>(enchantmentHolders.size());
        for (Holder<Enchantment> holder : enchantmentHolders) {
            enchantments.add(new RawEnchantment(
                    holder.getRegisteredName(),
                    itemStack.getEnchantments().getLevel(holder)
            ));
        }

        return new RawItemStack(
                aplicarPrimerMatch(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString(), NAMESPACES_ALIASES),
                customName,
                itemStack.getCount(),
                enchantments,
                slot,
                itemStack.isStackable()
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
                isStackable
        };
    }
}
