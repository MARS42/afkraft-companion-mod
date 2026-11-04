package com.mars4two.ikang.core.snapshots.model;

import java.util.List;

/**
 * Container holding the list of active effects for a player.
 */
public record RawPlayerEffects(List<RawPlayerEffect> effects) implements RawObject {

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                effects
        };
    }
}
