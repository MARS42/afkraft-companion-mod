package com.mars4two.ikang.core.snapshots.model;

/**
 * Immutable raw snapshot of a player's weapon attack cadence and recharge efficiency.
 */
public record RawPlayerAttackCadence(
        String status,
        float avgIntervalTicks,
        float requiredDelayTicks,
        int avgChargePercent,
        int fullChargeRate,
        int sweepRate
) implements RawObject {

    public static final String STATUS_IDLE = "IDLE";
    public static final String STATUS_OPTIMAL = "OPTIMAL";
    public static final String STATUS_TOO_FAST = "TOO_FAST";
    public static final String STATUS_NO_SWEEP = "NO_SWEEP";

    public static RawPlayerAttackCadence idle(float requiredDelayTicks) {
        return new RawPlayerAttackCadence(
                STATUS_IDLE,
                0.0f,
                requiredDelayTicks,
                0,
                0,
                0
        );
    }

    @Override
    public Object[] payloadValues() {
        return new Object[]{
                status,
                avgIntervalTicks,
                requiredDelayTicks,
                avgChargePercent,
                fullChargeRate,
                sweepRate
        };
    }
}
