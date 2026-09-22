package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.util.PlayerStatsContainer;

import java.util.Set;
import java.util.UUID;

public record PlayerEffectStatsSnapshot(Set<UUID> playerIds, String data) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerEffectStats";
    }

    @Override
    public String key() {
        return playerIds.toString() + getSnapshotName();
    }

    @Override
    public String changeToken() {
        return data;
    }

    public static PlayerEffectStatsSnapshot of(PlayerStatsContainer player) {
        return
                new PlayerEffectStatsSnapshot(Set.of(player.afkraft_companion$getProfileId()),player.afkraft_companion$getEffects());
    }
}
