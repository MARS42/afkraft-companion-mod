package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.util.PlayerStatsContainer;

import java.util.Map;

public record PlayerStatsSnapshot(String playerId, Map<String, String> data) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerStats";
    }

    @Override
    public String key() {
        return playerId;
    }

    @Override
    public String changeToken() {
        return playerId + ":" + String.join(",", data.values());
    }

    public static PlayerStatsSnapshot of(PlayerStatsContainer player) {
        return new PlayerStatsSnapshot(player.afkraft_companion$getProfileId().toString(), player.afkraft_companion$getStats());
    }
}
