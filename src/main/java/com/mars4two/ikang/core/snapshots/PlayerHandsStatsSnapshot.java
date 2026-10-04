package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawPlayerHands;
import com.mars4two.ikang.core.util.PlayerStatsContainer;

import java.util.Set;
import java.util.UUID;

public record PlayerHandsStatsSnapshot(Set<UUID> playerIds, RawPlayerHands data) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerHandsStats";
    }

    @Override
    public String key() {
        return playerIds.toString() + getSnapshotName();
    }

    public static PlayerHandsStatsSnapshot of(PlayerStatsContainer player) {
        return new PlayerHandsStatsSnapshot(Set.of(player.afkraft_companion$getProfileId()), player.afkraft_companion$getHandsStats());
    }
}
