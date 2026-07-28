package com.mars4two.ikang.client.core.snapshots;

public record PlayerStatsSnapshot(String gameMode, float health, float maxHealth, int foodLevel, double xPos, double yPos, double zPos) implements SnapshotBase{
    @Override
    public String getSnapshotName() {
        return "playerStats";
    }
}
