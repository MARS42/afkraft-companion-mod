package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.snapshots.model.RawPlayerAttackCadence;
import com.mars4two.ikang.core.util.PlayerStatsContainer;

import java.util.Set;
import java.util.UUID;

public record PlayerAttackCadenceSnapshot(Set<UUID> playerIds, RawPlayerAttackCadence data) implements SnapshotBase {

    @Override
    public String getSnapshotName() {
        return SnapshotType.PLAYER_ATTACK_CADENCE.snapshotName();
    }

    @Override
    public String key() {
        return playerIds.toString() + getSnapshotName();
    }

    public static PlayerAttackCadenceSnapshot of(PlayerStatsContainer player) {
        return new PlayerAttackCadenceSnapshot(
                Set.of(player.profileId()),
                player.attackCadence()
        );
    }
}
