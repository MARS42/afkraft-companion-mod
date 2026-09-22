package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
import java.util.stream.Collectors;

import static com.mars4two.ikang.core.util.PrimitiveJsonUtils.key;

@Mixin(ServerPlayer.class)
public abstract class PlayerSnapshotMixin extends Player implements PlayerStatsContainer {

    @Unique
    private static final SnapshotIngestManager ingestManager = SnapshotIngestManager.getInstance();

    protected PlayerSnapshotMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Override
    public UUID afkraft_companion$getProfileId() {
        return getGameProfile().id();
    }

    @Override
    public String afkraft_companion$getStats() {
        return "gm=" + (gameMode() != null ? gameMode().getName() : "") +
                "|h=" + getHealth() +
                "|mh=" + getMaxHealth() +
                "|fl=" + getFoodData().getFoodLevel() +
                "|exp=" + experienceLevel +
                "|expP=" + experienceProgress +
                "|x=" + getX() +
                "|y=" + getY() +
                "|z=" + getZ();
    }

    @Override
    public String afkraft_companion$getEffects() {
        return buildPlayerStatsEffects();
    }

    @Override
    public String afkraft_companion$getMainHand() {
        return buildPlayerStatsHand(InteractionHand.MAIN_HAND);
    }

    @Override
    public String afkraft_companion$getOffHand() {
        return buildPlayerStatsHand(InteractionHand.OFF_HAND);
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

    @Unique
    private String buildPlayerStatsEffects(){
        return "[" + getActiveEffects().stream().map(effect -> "{" +
                key("e", effect.getDescriptionId(), true) +
                key("a", effect.getAmplifier(), true) +
                key("d", effect.getDuration(), true) +
                key("am", effect.isAmbient(), true) +
                key("v", effect.isVisible(), false) +
                "}").collect(Collectors.joining(", ")) + "]";
    }

    @Unique
    private String buildPlayerStatsHand(InteractionHand hand) {
        var stack = getItemInHand(hand);
        if(stack.isEmpty()){
            return null;
        }

        return "{" +
                key("i", stack.getItemName().getString(), true) +
                key("c", stack.getCount(), true) +
                key("e", stack.getEnchantments()
                        .keySet().stream().map(Holder::getRegisteredName)
                        .collect(Collectors.joining(", ")), false) +
                "}";
    }
}
