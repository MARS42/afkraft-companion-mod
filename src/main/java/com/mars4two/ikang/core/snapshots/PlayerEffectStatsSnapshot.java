package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.util.PlayerStatsContainer;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.UUID;

public record PlayerEffectStatsSnapshot(Set<UUID> playerIds, PlayerEffectsData data) implements SnapshotBase {

    public record PlayerEffectsData (UUID playerId, String effects){
        @Override
        public @NonNull String toString() {
            return playerId + ":" + effects;
        }
    }

    @Override
    public String getSnapshotName() {
        return "playerEffectStats";
    }

    @Override
    public String key() {
        return data.playerId + getSnapshotName();
    }

    @Override
    public String changeToken() {
        return data.toString();
    }

    public static PlayerEffectStatsSnapshot of(PlayerStatsContainer player) {
        return
                new PlayerEffectStatsSnapshot(Set.of(player.afkraft_companion$getProfileId()),
                        new PlayerEffectsData(
                                player.afkraft_companion$getProfileId(),
                                player.afkraft_companion$getEffects()));
    }
}
