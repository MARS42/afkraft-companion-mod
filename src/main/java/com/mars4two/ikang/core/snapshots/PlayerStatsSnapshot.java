package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.util.PlayerStatsContainer;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record PlayerStatsSnapshot(Set<UUID> playerIds, PlayerStatsData data) implements SnapshotBase {

    public record PlayerStatsData(UUID playerId, Map<String, String> stats) {
        @Override
        public @NonNull String toString() {
            return playerId.toString() + ":" + String.join(",", stats.values());
        }
    }

    @Override
    public String getSnapshotName() {
        return "playerStats";
    }

    @Override
    public String key() {
        return data.playerId.toString() + getSnapshotName();
    }

    @Override
    public String changeToken() {
        return data.toString();
    }

    public static PlayerStatsSnapshot of(PlayerStatsContainer player) {
        return new PlayerStatsSnapshot(Set.of(player.afkraft_companion$getProfileId()), new PlayerStatsData(
                player.afkraft_companion$getProfileId(),
                player.afkraft_companion$getStats()
        ));
    }
}
