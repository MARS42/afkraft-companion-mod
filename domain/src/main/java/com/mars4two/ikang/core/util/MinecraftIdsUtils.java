package com.mars4two.ikang.core.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MinecraftIdsUtils {
    private static final Map<Pattern, String> EFFECT_NAMESPACES_ALIASES = Map.of(
            Pattern.compile("effect\\.minecraft"), "e.m"
    );
    private static final Map<Pattern, String> ITEM_NAMESPACES_ALIASES = Map.of(
            Pattern.compile("minecraft"), "i.m"
    );

    private static final Map<Pattern, String> BLOCK_NAMESPACES_ALIASES = Map.of(
            Pattern.compile("minecraft"), "b.m"
    );

    private MinecraftIdsUtils() {
        // Utility class, no instances allowed
    }

    private static String applyFirstMatch(String value, Map<Pattern, String> reglas) {
        if (value == null || reglas == null) {
            return value;
        }
        for (Map.Entry<Pattern, String> regla : reglas.entrySet()) {
            Matcher matcher = regla.getKey().matcher(value);
            if (matcher.find()) {
                return matcher.replaceAll(regla.getValue());
            }
        }
        return value;
    }

    public static String effectAlias(String descriptionId) {
        return applyFirstMatch(descriptionId, EFFECT_NAMESPACES_ALIASES);
    }

    public static String itemAlias(String itemId) {
        return applyFirstMatch(itemId, ITEM_NAMESPACES_ALIASES);
    }

    public static String blockAlias(String blockId) {
        return applyFirstMatch(blockId, BLOCK_NAMESPACES_ALIASES);
    }
}
