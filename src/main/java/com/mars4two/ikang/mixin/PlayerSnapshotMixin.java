package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.snapshots.model.*;
import com.mars4two.ikang.core.util.AttackCadenceTracker;
import com.mars4two.ikang.core.util.MobSphereCalculator;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

    @Unique
    private final AttackCadenceTracker afkraft_companion$cadenceTracker = new AttackCadenceTracker();

    @Unique
    private int afkraft_companion$lastEntityAttackTick = -1;

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

    @Override
    public RawPlayerAttackCadence afkraft_companion$getAttackCadence() {
        boolean holdingSword = this.getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS);
        return afkraft_companion$cadenceTracker.snapshot(
                this.tickCount,
                this.getCurrentItemAttackStrengthDelay(),
                holdingSword
        );
    }

    @Override
    public RawMobSphere afkraft_companion$getMobSphere() {
        return MobSphereCalculator.calculate((ServerPlayer) (Object) this);
    }

    @Override
    public void attack(Entity target) {
        if (!afkraft_companion$cannotAttack(target)) {
            int elapsedTicks = this.attackStrengthTicker;
            float requiredDelayTicks = this.getCurrentItemAttackStrengthDelay();
            float chargeScale = this.getAttackStrengthScale(0.5F);
            boolean fullCharge = chargeScale > 0.9F;
            boolean holdingSword = this.getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS);
            boolean sweepTriggered = afkraft_companion$willSweep(target, fullCharge, holdingSword);

            this.afkraft_companion$lastEntityAttackTick = this.tickCount;
            this.afkraft_companion$cadenceTracker.recordAttack(
                    this.tickCount,
                    elapsedTicks,
                    requiredDelayTicks,
                    chargeScale,
                    fullCharge,
                    true,
                    sweepTriggered,
                    holdingSword
            );
            ingestManager.ingestPlayerAttackCadence(this);
        }
        super.attack(target);
    }

    @Inject(at = @At("HEAD"), method = "swing")
    private void onSwing(InteractionHand hand, CallbackInfo ci) {
        if (hand != InteractionHand.MAIN_HAND || this.tickCount == this.afkraft_companion$lastEntityAttackTick) {
            return;
        }

        int elapsedTicks = this.attackStrengthTicker;
        float requiredDelayTicks = this.getCurrentItemAttackStrengthDelay();
        float chargeScale = this.getAttackStrengthScale(0.5F);
        boolean fullCharge = chargeScale > 0.9F;
        boolean holdingSword = this.getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS);

        this.afkraft_companion$cadenceTracker.recordAttack(
                this.tickCount,
                elapsedTicks,
                requiredDelayTicks,
                chargeScale,
                fullCharge,
                false,
                false,
                holdingSword
        );
        ingestManager.ingestPlayerAttackCadence(this);
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
        if ((this.tickCount + this.getId()) % 20 != 0) {
            return;
        }

        ingestManager.ingestPlayerStats(this);
        ingestManager.ingestPlayerMobSphere(this);
    }

    @Unique
    private boolean afkraft_companion$cannotAttack(Entity entity) {
        return !entity.isAttackable() || entity.skipAttackInteraction(this);
    }

    @Unique
    private boolean afkraft_companion$canCriticalAttack(Entity entity) {
        return this.fallDistance > 0.0
                && !this.onGround()
                && !this.onClimbable()
                && !this.isInWater()
                && !this.isMobilityRestricted()
                && !this.isPassenger()
                && entity instanceof LivingEntity
                && !this.isSprinting();
    }

    @Unique
    private boolean afkraft_companion$willSweep(Entity entity, boolean fullStrengthAttack, boolean holdingSword) {
        if (!holdingSword) {
            return false;
        }
        boolean knockbackAttack = this.isSprinting() && fullStrengthAttack;
        boolean criticalAttack = fullStrengthAttack && afkraft_companion$canCriticalAttack(entity);
        if (fullStrengthAttack && !criticalAttack && !knockbackAttack && this.onGround()) {
            double approximateSpeedSq = this.getKnownMovement().horizontalDistanceSqr();
            double maxSpeedForSweepAttack = this.getSpeed() * 2.5;
            return approximateSpeedSq < Mth.square(maxSpeedForSweepAttack);
        }
        return false;
    }

    @Unique
    private RawItemStack extractHandItem(InteractionHand hand) {
        var stack = getItemInHand(hand);
        if (stack.isEmpty()) {
            return RawItemStack.EMPTY;
        }
        return RawItemStack.from(hand.asEquipmentSlot().getId(), stack);
    }
}
