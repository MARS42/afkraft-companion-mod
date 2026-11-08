package com.mars4two.ikang.core.storage;

/**
 * Pure domain container tracking configuration entry.
 */
public record ContainerTrackingEntry(boolean enabled, ContainerTrackingMode mode) {

    public ContainerTrackingEntry {
        if (mode == null || !enabled) {
            mode = ContainerTrackingMode.OFF;
            enabled = false;
        }
    }

    public ContainerTrackingEntry(boolean enabled) {
        this(enabled, enabled ? ContainerTrackingMode.CONTAINER : ContainerTrackingMode.OFF);
    }

    public ContainerTrackingEntry(ContainerTrackingMode mode) {
        this(mode != null && mode.isEnabled(), mode != null ? mode : ContainerTrackingMode.OFF);
    }

    public static final ContainerTrackingEntry OFF = new ContainerTrackingEntry(false, ContainerTrackingMode.OFF);
}
