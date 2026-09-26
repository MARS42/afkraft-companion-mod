package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.snapshots.model.RawHandItem;
import com.mars4two.ikang.core.snapshots.model.RawPlayerEffect;
import com.mars4two.ikang.core.snapshots.model.RawPlayerEffects;
import com.mars4two.ikang.core.snapshots.model.RawPlayerHands;
import com.mars4two.ikang.core.snapshots.model.RawPlayerStats;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public RawPlayerStats afkraft_companion$getStats() {
        return new RawPlayerStats(
                gameMode() != null ? gameMode().getName() : "",
                getHealth(),
                getMaxHealth(),
                getFoodData().getFoodLevel(),
                experienceLevel,
                experienceProgress,
                getX(),
                getY(),
                getZ()
        );
    }

    @Override
    public RawPlayerEffects afkraft_companion$getEffects() {
        var active = getActiveEffects();
        if (active.isEmpty()) {
            return new RawPlayerEffects(List.of());
        }
        List<RawPlayerEffect> list = new ArrayList<>(active.size());
        for (var effect : active) {

            float tps = this.level().tickRateManager().tickrate();
            int durationTicks = effect.getDuration();
            int durationSeconds = (durationTicks > 0) ? (int) (durationTicks / tps) : durationTicks;

            list.add(new RawPlayerEffect(
                    effect.getDescriptionId(),
                    effect.getAmplifier(),
                    durationSeconds,
                    effect.isAmbient(),
                    effect.isVisible()
            ));
        }
        return new RawPlayerEffects(list);
    }

    @Override
    public RawPlayerHands afkraft_companion$getHandsStats() {
        return new RawPlayerHands(
                extractHandItem(InteractionHand.MAIN_HAND),
                extractHandItem(InteractionHand.OFF_HAND)
        );
    }

    @Inject(at = @At("TAIL"), method = "setExperienceLevels")
    private void onSetExperienceLevels(int value, CallbackInfo ci) {
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "setExperiencePoints")
    private void onSetExperiencePoints(int value, CallbackInfo ci) {
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "giveExperienceLevels")
    private void onGiveExperienceLevels(int value, CallbackInfo ci) {
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "giveExperiencePoints")
    private void onGiveExperiencePoints(int value, CallbackInfo ci) {
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "hurtServer")
    private void onHurtServer(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        ingestManager.ingestPlayerStats(this);
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo ci) {
        if (this.tickCount % 20 != 0) {
            return;
        }

        ingestManager.ingestPlayerStats(this);
    }

    @Unique
    private RawHandItem extractHandItem(InteractionHand hand) {
        var stack = getItemInHand(hand);
        if (stack.isEmpty()) {
            return null;
        }

        var enchantmentHolders = stack.getEnchantments().keySet();
        List<String> enchantments = new ArrayList<>(enchantmentHolders.size());
        for (Holder<?> holder : enchantmentHolders) {
            enchantments.add(holder.getRegisteredName());
        }

        return new RawHandItem(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCustomName() != null ? stack.getCustomName().getString() : "",
                stack.getCount(),
                enchantments
        );
    }
}
