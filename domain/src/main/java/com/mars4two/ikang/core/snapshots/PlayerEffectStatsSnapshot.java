package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawPlayerEffects;
import com.mars4two.ikang.core.util.PlayerStatsContainer;

import java.util.Set;
import java.util.UUID;

public record PlayerEffectStatsSnapshot(Set<UUID> playerIds, RawPlayerEffects data) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return "playerEffectStats";
    }

    @Override
    public String key() {
        return playerIds.toString() + getSnapshotName();
    }

    public static PlayerEffectStatsSnapshot of(PlayerStatsContainer player) {
        return new PlayerEffectStatsSnapshot(Set.of(player.profileId()), player.effects());
    }
}
