package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.util.PlayerStatsContainer;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.UUID;

public record PlayerHandsStatsSnapshot(Set<UUID> playerIds, PlayerHandsStatsData data) implements SnapshotBase {

    public record PlayerHandsStatsData(UUID playerId, String mainHand, String offHand){
        @Override
        public @NonNull String toString() {
            return playerId + ":" + mainHand + ":" + offHand;
        }
    }

    @Override
    public String getSnapshotName() {
        return "playerHandsStats";
    }

    @Override
    public String key() {
        return data.playerId + getSnapshotName();
    }

    @Override
    public String changeToken() {
        return data.toString();
    }

    public static PlayerHandsStatsSnapshot of(PlayerStatsContainer player) {
        return
                new PlayerHandsStatsSnapshot(Set.of(player.afkraft_companion$getProfileId()), new PlayerHandsStatsData(
                        player.afkraft_companion$getProfileId(),
                        player.afkraft_companion$getMainHand(),
                        player.afkraft_companion$getOffHand()
                ));
    }
}
