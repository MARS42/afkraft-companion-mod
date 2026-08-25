package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class PlayerSnapshotMixin extends Player implements PlayerStatsContainer {

    @Unique
    private static final SnapshotIngestManager ingestManager = SnapshotIngestManager.getInstance();

    @Unique
    private final Map<String, String> playerStatsMap = new HashMap<>();

    protected PlayerSnapshotMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Override
    public UUID afkraft_companion$getProfileId() {
        return getUUID();
    }

    @Override
    public Map<String, String> afkraft_companion$getStats() {
        playerStatsMap.put("gameMode", gameMode() != null ? gameMode().getName() : "");
        playerStatsMap.put("health", String.valueOf(getHealth()));
        playerStatsMap.put("maxHealth", String.valueOf(getMaxHealth()));
        playerStatsMap.put("foodLevel", String.valueOf(getFoodData().getFoodLevel()));
        playerStatsMap.put("experience", String.valueOf(experienceLevel));
        playerStatsMap.put("xPos", String.valueOf(getX()));
        playerStatsMap.put("yPos", String.valueOf(getY()));
        playerStatsMap.put("zPos", String.valueOf(getZ()));

        return playerStatsMap;
    }

    @Inject(at = @At("TAIL"), method = "setExperienceLevels")
    private void onSetExperienceLevels(int value, CallbackInfo ci){
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "setExperiencePoints")
    private void onSetExperiencePoints(int value, CallbackInfo ci){
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "giveExperienceLevels")
    private void onGiveExperienceLevels(int value, CallbackInfo ci){
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "giveExperiencePoints")
    private void onGiveExperiencePoints(int value, CallbackInfo ci){
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "hurtServer")
    private void onHurtServer(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir){
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo ci){
        if (this.tickCount % 20 != 0) {
            return;
        }

        ingestManager.ingestPlayerStats(this);
    }
}
