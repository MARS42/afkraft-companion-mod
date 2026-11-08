package com.mars4two.ikang.core.storage;

/**
 * Pure domain tracking mode for containers and AFK stations.
 * Serialized names must remain stable across world saves and network packets.
 */
public enum ContainerTrackingMode {
    OFF("off"),
    CONTAINER("container"),
    PORTABLE_CONTAINER("portable_container"),
    LOAD_STATION("load_station");

    private final String serializedName;

    ContainerTrackingMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public boolean isEnabled() {
        return this != OFF;
    }

    public static ContainerTrackingMode fromSerializedName(String name) {
        if (name == null) {
            return OFF;
        }
        for (ContainerTrackingMode mode : values()) {
            if (mode.serializedName.equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return OFF;
    }
}
