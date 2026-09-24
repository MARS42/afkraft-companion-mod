package com.mars4two.ikang.core.snapshots.model;

import java.util.List;

/**
 * Container holding the list of active effects for a player.
 */
public record RawPlayerEffects(List<RawPlayerEffect> effects) {

    public String toPayloadString() {
        if (effects == null || effects.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < effects.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(effects.get(i).toPayloadString());
        }
        sb.append("]");
        return sb.toString();
    }
}
