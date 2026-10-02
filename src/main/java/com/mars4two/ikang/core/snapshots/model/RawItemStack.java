package com.mars4two.ikang.core.snapshots.model;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RawItemStack(
        String displayName,
        int count,
        int index
) {
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
    public static RawItemStack EMPTY = new RawItemStack("", 0, 0);

    public static RawItemStack from(int slot, ItemStack itemStack) {
        var displayName = itemStack.getCustomName() != null ?
                itemStack.getCustomName().getString()
                : aplicarPrimerMatch(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString(), NAMESPACES_ALIASES);

        return new RawItemStack(
                displayName,
                itemStack.getCount(),
                slot
        );
    }

    public String toPayloadString() {
        return "\"n=" + new JsonPrimitive(displayName) +
                "|c=" + count +
                "|i=" + index + "\"";
    }
}
