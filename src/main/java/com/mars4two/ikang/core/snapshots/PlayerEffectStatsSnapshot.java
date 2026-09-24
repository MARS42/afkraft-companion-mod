package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawPlayerEffects;
import com.mars4two.ikang.core.util.PlayerStatsContainer;

import java.util.Set;
import java.util.UUID;

public record PlayerEffectStatsSnapshot(Set<UUID> playerIds, RawPlayerEffects effects) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerEffectStats";
    }

    @Override
    public String key() {
        return playerIds.toString() + getSnapshotName();
    }

    @Override
    public Object changeToken() {
        return effects;
    }

    @Override
    public String data() {
        return effects != null ? effects.toPayloadString() : "[]";
    }

    public static PlayerEffectStatsSnapshot of(PlayerStatsContainer player) {
        return new PlayerEffectStatsSnapshot(Set.of(player.afkraft_companion$getProfileId()), player.afkraft_companion$getEffects());
    }
}
