package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.core.snapshots.model.RawMobSphere;
import com.mars4two.ikang.core.snapshots.model.RawPlayerAttackCadence;
import com.mars4two.ikang.core.snapshots.model.RawPlayerEffect;
import com.mars4two.ikang.core.snapshots.model.RawPlayerEffects;
import com.mars4two.ikang.core.snapshots.model.RawPlayerHands;
import com.mars4two.ikang.core.snapshots.model.RawPlayerStats;
import com.mars4two.ikang.core.util.AttackCadenceTracker;
import com.mars4two.ikang.core.util.ItemStackMapper;
import com.mars4two.ikang.core.util.MobSphereCalculator;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import com.mars4two.ikang.core.util.ServerPlayerAttackHook;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import com.mojang.authlib.GameProfile;
//? if >=1.21.2 {
import net.minecraft.server.level.ServerLevel;
//?}
//? if <1.21.6 {
/*import net.minecraft.core.BlockPos;
*///?}
//? if <1.21.9 {
/*import net.minecraft.world.effect.MobEffects;
*///?}
//? if >=26.3 {
import net.minecraft.world.item.component.SwingAnimation;
//?}
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
public abstract class PlayerSnapshotMixin extends Player implements ServerPlayerAttackHook {

    @Unique
    private static final SnapshotIngestManager ingestManager = SnapshotIngestManager.getInstance();

    @Unique
    private final AttackCadenceTracker afkraft_companion$cadenceTracker = new AttackCadenceTracker();

    @Unique
    private int afkraft_companion$lastEntityAttackTick = -1;

    @Unique
    private final PlayerStatsContainer afkraft_companion$statsView = new PlayerStatsContainer() {
        @Override
        public UUID profileId() {
            return WorldAndEntityAdapter.profileId(getGameProfile());
        }

        @Override
        public RawPlayerStats stats() {
            ServerPlayer self = (ServerPlayer) (Object) PlayerSnapshotMixin.this;
            //? if >=1.21.5 {
            String gameModeName = self.gameMode() != null ? self.gameMode().getName() : "";
            //?} else {
            /*String gameModeName = self.gameMode.getGameModeForPlayer() != null
                    ? self.gameMode.getGameModeForPlayer().getName()
                    : "";
            *///?}
            return new RawPlayerStats(
                    gameModeName,
                    getHealth(),
                    getMaxHealth(),
                    getFoodData().getFoodLevel(),
                    Math.max(0, getAirSupply()),
                    experienceLevel,
                    experienceProgress,
                    getX(),
                    getY(),
                    getZ(),
                    getYRot(),
                    getXRot()
            );
        }

        @Override
        public RawPlayerEffects effects() {
            var active = getActiveEffects();
            if (active.isEmpty()) {
                return new RawPlayerEffects(List.of());
            }
            List<RawPlayerEffect> list = new ArrayList<>(active.size());
            float tps = WorldAndEntityAdapter.tickRate(PlayerSnapshotMixin.this.level());
            for (var effect : active) {
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
        public RawPlayerHands handsStats() {
            return new RawPlayerHands(
                    extractHandItem(InteractionHand.MAIN_HAND),
                    extractHandItem(InteractionHand.OFF_HAND)
            );
        }

        @Override
        public RawPlayerAttackCadence attackCadence() {
            boolean holdingSword = PlayerSnapshotMixin.this.getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS);
            return afkraft_companion$cadenceTracker.snapshot(
                    PlayerSnapshotMixin.this.tickCount,
                    PlayerSnapshotMixin.this.getCurrentItemAttackStrengthDelay(),
                    holdingSword
            );
        }

        @Override
        public RawMobSphere mobSphere() {
            return MobSphereCalculator.calculate((ServerPlayer) (Object) PlayerSnapshotMixin.this);
        }
    };

    //? if >=1.21.6 {
    protected PlayerSnapshotMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }
    //?} else {
    /*protected PlayerSnapshotMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }
    *///?}

    @Override
    public void afkraft_companion$onBeforeAttack(Entity target) {
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
            ingestManager.ingestPlayerAttackCadence(afkraft_companion$statsView);
        }
    }

    @Override
    public void afkraft_companion$onBeforeSwing(InteractionHand hand) {
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
        ingestManager.ingestPlayerAttackCadence(afkraft_companion$statsView);
    }

    //? if >=26.3 {
    @Inject(at = @At("HEAD"), method = "swingAndResetAttackStrength")
    private void onSwing(InteractionHand hand, SwingAnimation animation, boolean fromServer, CallbackInfo ci) {
        afkraft_companion$onBeforeSwing(hand);
    }
    //?} else {
    /*@Inject(at = @At("HEAD"), method = "swing")
    private void onSwing(InteractionHand hand, CallbackInfo ci) {
        afkraft_companion$onBeforeSwing(hand);
    }
    *///?}

    @Inject(at = @At("TAIL"), method = "setExperienceLevels")
    private void onSetExperienceLevels(int value, CallbackInfo ci) {
        ingestManager.ingestPlayerStats(afkraft_companion$statsView);
    }

    @Inject(at = @At("TAIL"), method = "setExperiencePoints")
    private void onSetExperiencePoints(int value, CallbackInfo ci) {
        ingestManager.ingestPlayerStats(afkraft_companion$statsView);
    }

    @Inject(at = @At("TAIL"), method = "giveExperienceLevels")
    private void onGiveExperienceLevels(int value, CallbackInfo ci) {
        ingestManager.ingestPlayerStats(afkraft_companion$statsView);
    }

    @Inject(at = @At("TAIL"), method = "giveExperiencePoints")
    private void onGiveExperiencePoints(int value, CallbackInfo ci) {
        ingestManager.ingestPlayerStats(afkraft_companion$statsView);
    }

    //? if >=1.21.2 {
    @Inject(at = @At("TAIL"), method = "hurtServer")
    private void onHurtServer(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        ingestManager.ingestPlayerStats(afkraft_companion$statsView);
    }
    //?} else {
    /*@Inject(at = @At("TAIL"), method = "hurt")
    private void onHurt(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        ingestManager.ingestPlayerStats(afkraft_companion$statsView);
    }
    *///?}

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo ci) {
        if ((this.tickCount + this.getId()) % 20 != 0) {
            return;
        }

        ingestManager.ingestPlayerStats(afkraft_companion$statsView);
        ingestManager.ingestPlayerMobSphere(afkraft_companion$statsView);
    }

    @Unique
    private boolean afkraft_companion$cannotAttack(Entity entity) {
        return !entity.isAttackable() || entity.skipAttackInteraction(this);
    }

    @Unique
    private boolean afkraft_companion$canCriticalAttack(Entity entity) {
        //? if >=1.21.9 {
        boolean mobilityRestricted = this.isMobilityRestricted();
        //?} else {
        /*boolean mobilityRestricted = this.hasEffect(MobEffects.BLINDNESS);
        *///?}
        return this.fallDistance > 0.0
                && !this.onGround()
                && !this.onClimbable()
                && !this.isInWater()
                && !mobilityRestricted
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
        return ItemStackMapper.from(WorldAndEntityAdapter.handEquipmentSlotIndex(hand), stack);
    }
}
