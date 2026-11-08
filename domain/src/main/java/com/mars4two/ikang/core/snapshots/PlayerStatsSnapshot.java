package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawPlayerStats;
import com.mars4two.ikang.core.util.PlayerStatsContainer;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record PlayerStatsSnapshot(Set<UUID> playerIds, RawPlayerStats data) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerStats";
    }

    @Override
    public String key() {
        return playerIds.stream().map(UUID::toString).collect(Collectors.joining()) + getSnapshotName();
    }

    public static PlayerStatsSnapshot of(PlayerStatsContainer player) {
        return new PlayerStatsSnapshot(Set.of(player.profileId()), player.stats());
    }
}
