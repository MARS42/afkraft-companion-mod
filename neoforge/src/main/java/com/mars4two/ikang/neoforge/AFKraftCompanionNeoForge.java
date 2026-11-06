package com.mars4two.ikang.neoforge;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.neoforge.platform.NeoForgeNetworkBridge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(AFKraftCompanion.MOD_ID)
public final class AFKraftCompanionNeoForge {

    public AFKraftCompanionNeoForge(IEventBus modEventBus) {
        AFKraftCompanion.init();
        modEventBus.addListener(NeoForgeNetworkBridge::onRegisterPayloadHandlers);
    }
}
