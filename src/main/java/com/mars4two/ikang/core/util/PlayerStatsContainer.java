package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawPlayerEffects;
import com.mars4two.ikang.core.snapshots.model.RawPlayerHands;
import com.mars4two.ikang.core.snapshots.model.RawPlayerStats;

import java.util.UUID;

public interface PlayerStatsContainer {
    UUID afkraft_companion$getProfileId();
    RawPlayerStats afkraft_companion$getStats();
    RawPlayerEffects afkraft_companion$getEffects();
    RawPlayerHands afkraft_companion$getHandsStats();
}
