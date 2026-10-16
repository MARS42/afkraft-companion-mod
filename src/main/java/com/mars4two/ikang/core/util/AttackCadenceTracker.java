package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawPlayerAttackCadence;

/**
 * Sliding-window tracker for server-side player attack cadence, weapon recharge, and sweep efficiency.
 * Quantizes output metrics so steady AFK cadences deduplicate cleanly in {@link Conflater}.
 */
public final class AttackCadenceTracker {

    public static final int WINDOW_SIZE = 10;
    public static final int IDLE_TIMEOUT_TICKS = 100;
    private static final int OPTIMAL_RATE_THRESHOLD = 90;

    private final int[] intervalTicksBuffer = new int[WINDOW_SIZE];
    private final float[] chargeScaleBuffer = new float[WINDOW_SIZE];
    private final boolean[] fullChargeBuffer = new boolean[WINDOW_SIZE];
    private final boolean[] entityHitBuffer = new boolean[WINDOW_SIZE];
    private final boolean[] sweepBuffer = new boolean[WINDOW_SIZE];

    private int head = 0;
    private int count = 0;
    private int lastActionTick = -1;
    private boolean lastHoldingSword = false;

    public synchronized void recordAttack(
            int currentTick,
            int elapsedTicks,
            float requiredDelayTicks,
            float chargeScale,
            boolean fullCharge,
            boolean entityHit,
            boolean sweepTriggered,
            boolean holdingSword
    ) {
        boolean wasIdle = isIdle(currentTick);
        if (wasIdle) {
            resetWindow();
        }

        // The first strike after an idle period has an arbitrarily large attackStrengthTicker;
        // mark its interval as -1 so only consecutive follow-up swings contribute to avgIntervalTicks.
        intervalTicksBuffer[head] = wasIdle ? -1 : elapsedTicks;
        chargeScaleBuffer[head] = chargeScale;
        fullChargeBuffer[head] = fullCharge;
        entityHitBuffer[head] = entityHit;
        sweepBuffer[head] = sweepTriggered;

        head = (head + 1) % WINDOW_SIZE;
        if (count < WINDOW_SIZE) {
            count++;
        }

        lastActionTick = currentTick;
        lastHoldingSword = holdingSword;
    }

    public synchronized RawPlayerAttackCadence snapshot(int currentTick, float currentRequiredDelayTicks, boolean currentlyHoldingSword) {
        float roundedRequiredDelay = roundOneDecimal(currentRequiredDelayTicks);

        if (count == 0 || isIdle(currentTick)) {
            if (count > 0) {
                resetWindow();
            }
            return RawPlayerAttackCadence.idle(roundedRequiredDelay);
        }

        int intervalSum = 0;
        int intervalSamples = 0;
        float chargeSum = 0.0f;
        int fullChargeCount = 0;
        int entityHitCount = 0;
        int sweepCount = 0;

        for (int i = 0; i < count; i++) {
            int interval = intervalTicksBuffer[i];
            if (interval >= 0) {
                intervalSum += interval;
                intervalSamples++;
            }
            chargeSum += chargeScaleBuffer[i];
            if (fullChargeBuffer[i]) {
                fullChargeCount++;
            }
            if (entityHitBuffer[i]) {
                entityHitCount++;
                if (sweepBuffer[i]) {
                    sweepCount++;
                }
            }
        }

        float avgIntervalTicks = (intervalSamples > 0)
                ? roundOneDecimal((float) intervalSum / intervalSamples)
                : roundedRequiredDelay;

        int avgChargePercent = Math.round((chargeSum / count) * 100.0f);
        int fullChargeRate = Math.round(((float) fullChargeCount / count) * 100.0f);
        int sweepRate = (entityHitCount > 0)
                ? Math.round(((float) sweepCount / entityHitCount) * 100.0f)
                : 0;

        boolean swordContext = currentlyHoldingSword || lastHoldingSword;
        String status = resolveStatus(fullChargeRate, sweepRate, entityHitCount, swordContext);

        return new RawPlayerAttackCadence(
                status,
                avgIntervalTicks,
                roundedRequiredDelay,
                avgChargePercent,
                fullChargeRate,
                sweepRate
        );
    }

    private boolean isIdle(int currentTick) {
        return lastActionTick < 0 || (currentTick - lastActionTick) > IDLE_TIMEOUT_TICKS;
    }

    private void resetWindow() {
        head = 0;
        count = 0;
        lastActionTick = -1;
    }

    private static String resolveStatus(int fullChargeRate, int sweepRate, int entityHitCount, boolean holdingSword) {
        if (fullChargeRate < OPTIMAL_RATE_THRESHOLD) {
            return RawPlayerAttackCadence.STATUS_TOO_FAST;
        }
        if (holdingSword && entityHitCount > 0 && sweepRate < OPTIMAL_RATE_THRESHOLD) {
            return RawPlayerAttackCadence.STATUS_NO_SWEEP;
        }
        return RawPlayerAttackCadence.STATUS_OPTIMAL;
    }

    private static float roundOneDecimal(float value) {
        return Math.round(value * 10.0f) / 10.0f;
    }
}
