package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawMobSphere;
import com.mars4two.ikang.core.snapshots.model.RawPlayerAttackCadence;
import com.mars4two.ikang.core.snapshots.model.RawPlayerEffects;
import com.mars4two.ikang.core.snapshots.model.RawPlayerHands;
import com.mars4two.ikang.core.snapshots.model.RawPlayerStats;

import java.util.UUID;

/**
 * Pure domain read-port over a player's status, active effects, held items, attack cadence, and mob sphere.
 */
public interface PlayerStatsContainer {
    UUID profileId();
    RawPlayerStats stats();
    RawPlayerEffects effects();
    RawPlayerHands handsStats();
    RawPlayerAttackCadence attackCadence();
    RawMobSphere mobSphere();
}
