package com.mars4two.ikang.core.snapshots;

import net.minecraft.world.entity.player.Player;

public record PlayerStatsSnapshot(String playerId, String gameMode, float health, float maxHealth, int foodLevel, double xPos, double yPos, double zPos) implements SnapshotBase {
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
        return playerId + gameMode + health + maxHealth + foodLevel + xPos + yPos + zPos;
    }

    public static PlayerStatsSnapshot of(Player player) {
        var gm = player.gameMode() != null ? player.gameMode().getName() : "";
        return new PlayerStatsSnapshot(player.getUUID().toString(), gm, player.getHealth(), player.getMaxHealth(), player.getFoodData().getFoodLevel(), player.getX(), player.getY(), player.getZ());
    }
}
