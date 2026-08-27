package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.util.PlayerStatsContainer;

public record PlayerHandsStatsSnapshot(String playerId, String mainHandData, String offHandData) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerHandsStats";
    }

    @Override
    public String key() {
        return playerId + getSnapshotName();
    }

    @Override
    public String changeToken() {
        return playerId + ":" + mainHandData + ":" + offHandData;
    }

    public static PlayerHandsStatsSnapshot of(PlayerStatsContainer player) {
        return
                new PlayerHandsStatsSnapshot(player.afkraft_companion$getProfileId().toString(),
                        player.afkraft_companion$getMainHand(),
                        player.afkraft_companion$getOffHand());
    }
}
