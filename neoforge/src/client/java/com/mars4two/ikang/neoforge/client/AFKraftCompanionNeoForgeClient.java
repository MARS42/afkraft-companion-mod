package com.mars4two.ikang.neoforge.client;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.client.AFKraftCompanionClient;
import com.mars4two.ikang.neoforge.client.platform.NeoForgeClientKeyMappingBridge;
import com.mars4two.ikang.neoforge.client.platform.NeoForgeClientNetworkHook;
import com.mars4two.ikang.neoforge.platform.NeoForgeNetworkBridge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = AFKraftCompanion.MOD_ID, dist = Dist.CLIENT)
public final class AFKraftCompanionNeoForgeClient {

    public AFKraftCompanionNeoForgeClient(IEventBus modEventBus) {
        NeoForgeNetworkBridge.installClientHook(new NeoForgeClientNetworkHook());
        AFKraftCompanionClient.init();
        modEventBus.addListener(NeoForgeClientKeyMappingBridge::onRegisterKeyMappings);
    }
}
