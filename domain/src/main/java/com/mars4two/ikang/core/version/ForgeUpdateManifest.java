package com.mars4two.ikang.core.version;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Data Transfer Object for Forge/NeoForge update JSON specification.
 */
public class ForgeUpdateManifest {

    @SerializedName("homepage")
    private String homepage;

    @SerializedName("promos")
    private Map<String, String> promos;

    public ForgeUpdateManifest() {
    }

    public ForgeUpdateManifest(String homepage, Map<String, String> promos) {
        this.homepage = homepage;
        this.promos = promos;
    }

    public String getHomepage() {
        return homepage != null && !homepage.isBlank() ? homepage : "https://github.com/MARS42/afkraft-companion-mod";
    }

    public Map<String, String> getPromos() {
        return promos != null ? promos : Collections.emptyMap();
    }

    /**
     * Resolves the latest version string recommended or latest for the given Minecraft version.
     * Matches exact key `mcVersion-latest`, then `mcVersion-recommended`.
     * If neither exists, searches for closest matching cluster prefix.
     */
    public Optional<String> resolveLatest(String mcVersion) {
        if (promos == null || promos.isEmpty() || mcVersion == null || mcVersion.isBlank()) {
            return Optional.empty();
        }

        String normalizedMc = mcVersion.trim();

        // 1. Exact match for mcVersion-latest
        String latest = promos.get(normalizedMc + "-latest");
        if (latest != null && !latest.isBlank()) {
            return Optional.of(latest.trim());
        }

        // 2. Exact match for mcVersion-recommended
        String recommended = promos.get(normalizedMc + "-recommended");
        if (recommended != null && !recommended.isBlank()) {
            return Optional.of(recommended.trim());
        }

        // 3. Cluster fallback: find promo key that starts with same major.minor
        String[] parts = normalizedMc.split("\\.");
        if (parts.length >= 2) {
            String prefix = parts[0] + "." + parts[1];
            for (Map.Entry<String, String> entry : promos.entrySet()) {
                if (entry.getKey().startsWith(prefix) && entry.getKey().endsWith("-latest")) {
                    return Optional.ofNullable(entry.getValue());
                }
            }
        }

        return Optional.empty();
    }
}
