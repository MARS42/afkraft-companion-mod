package com.mars4two.ikang.core.snapshots.model;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable raw effect data for an active mob effect.
 */
public record RawPlayerEffect(
        String descriptionId,
        int amplifier,
        int duration, // duration in seconds
        boolean ambient,
        boolean visible
) {
    private static final Map<Pattern, String> NAMESPACES_ALIASES = Map.of(
            Pattern.compile("effect\\.minecraft"), "m"
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

    public String toPayloadString() {
        return "\"e=" + aplicarPrimerMatch(descriptionId, NAMESPACES_ALIASES) +
                "|a=" + amplifier +
                "|d=" + duration +
                "|am=" + ambient +
                "|v=" + visible + "\"";
    }
}
