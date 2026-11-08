package com.mars4two.ikang.fabric;

import com.mars4two.ikang.AFKraftCompanion;
import net.fabricmc.api.ModInitializer;

public final class AFKraftCompanionFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        AFKraftCompanion.init();
    }
}
