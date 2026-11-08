package com.mars4two.ikang.core.snapshots.model;

import com.mars4two.ikang.core.util.MinecraftIdsUtils;

/**
 * Immutable raw effect data for an active mob effect.
 */
public record RawPlayerEffect(
        String descriptionId,
        int amplifier,
        int duration, // duration in seconds
        boolean ambient,
        boolean visible
) implements RawObject {
    @Override
    public Object[] payloadValues() {
        return new Object[]{MinecraftIdsUtils.effectAlias(descriptionId), amplifier, duration, ambient, visible};
    }
}
