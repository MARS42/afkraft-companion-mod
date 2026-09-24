package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawPlayerStats;
import com.mars4two.ikang.core.util.PlayerStatsContainer;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record PlayerStatsSnapshot(Set<UUID> playerIds, RawPlayerStats stats) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerStats";
    }

    @Override
    public String key() {
        return playerIds.stream().map(UUID::toString).collect(Collectors.joining()) + getSnapshotName();
    }

    @Override
    public Object changeToken() {
        return stats;
    }

    @Override
    public String data() {
        return stats != null ? stats.toPayloadString() : "";
    }

    public static PlayerStatsSnapshot of(PlayerStatsContainer player) {
        return new PlayerStatsSnapshot(Set.of(player.afkraft_companion$getProfileId()), player.afkraft_companion$getStats());
    }
}
