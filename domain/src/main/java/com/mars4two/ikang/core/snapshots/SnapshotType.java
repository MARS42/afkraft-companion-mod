package com.mars4two.ikang.core.snapshots;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical catalog of snapshot types produced by AFKraft Companion.
 * Order here defines the deterministic display order in the preferences UI.
 */
public enum SnapshotType {
    PLAYER_STATS("playerStats", "player_stats"),
    PLAYER_EFFECT_STATS("playerEffectStats", "player_effect_stats"),
    PLAYER_HANDS_STATS("playerHandsStats", "player_hands_stats"),
    PLAYER_ATTACK_CADENCE("playerAttackCadence", "player_attack_cadence"),
    PLAYER_HOTBAR("playerHotbar", "player_hotbar"),
    PLAYER_INVENTORY("playerInventory", "player_inventory"),
    PLAYER_EQUIPMENT("playerEquipment", "player_equipment"),
    PLAYER_MOB_SPHERE("playerMobSphere", "player_mob_sphere"),
    CONTAINER("container", "container"),
    LOAD_STATION("load-station", "load_station");

    private static final Map<String, SnapshotType> BY_SNAPSHOT_NAME;

    static {
        Map<String, SnapshotType> map = new LinkedHashMap<>();
        for (SnapshotType type : values()) {
            map.put(type.snapshotName, type);
        }
        BY_SNAPSHOT_NAME = Collections.unmodifiableMap(map);
    }

    private final String snapshotName;
    private final String translationSlug;

    SnapshotType(String snapshotName, String translationSlug) {
        this.snapshotName = snapshotName;
        this.translationSlug = translationSlug;
    }

    public String snapshotName() {
        return snapshotName;
    }

    public String translationSlug() {
        return translationSlug;
    }

    public static Optional<SnapshotType> fromSnapshotName(String rawName) {
        if (rawName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_SNAPSHOT_NAME.get(rawName.trim()));
    }

    public static boolean isKnownSnapshotName(String rawName) {
        return fromSnapshotName(rawName).isPresent();
    }

    /**
     * Returns an ordered map of all known snapshot types enabled by default.
     */
    public static Map<String, Boolean> defaultPreferencesMap() {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        for (SnapshotType type : values()) {
            defaults.put(type.snapshotName, Boolean.TRUE);
        }
        return Collections.unmodifiableMap(defaults);
    }

    /**
     * Merges a sparse or unsorted map onto the canonical catalog defaults.
     * Missing known snapshot keys default to {@code true}; unknown keys are ignored.
     */
    public static Map<String, Boolean> mergeWithDefaults(Map<String, Boolean> raw) {
        Map<String, Boolean> merged = new LinkedHashMap<>();
        for (SnapshotType type : values()) {
            boolean enabled = true;
            if (raw != null && raw.containsKey(type.snapshotName)) {
                Boolean value = raw.get(type.snapshotName);
                enabled = value == null || value;
            }
            merged.put(type.snapshotName, enabled);
        }
        return Collections.unmodifiableMap(merged);
    }
}
