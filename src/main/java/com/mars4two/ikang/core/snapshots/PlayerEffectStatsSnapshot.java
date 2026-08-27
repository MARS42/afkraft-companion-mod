package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.util.PlayerStatsContainer;

public record PlayerEffectStatsSnapshot(String playerId, String data) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerEffectStats";
    }

    @Override
    public String key() {
        return playerId + getSnapshotName();
    }

    @Override
    public String changeToken() {
        return playerId + ":" + data;
    }

    public static PlayerEffectStatsSnapshot of(PlayerStatsContainer player) {
        return
                new PlayerEffectStatsSnapshot(player.afkraft_companion$getProfileId().toString(), player.afkraft_companion$getEffects());
    }
}
