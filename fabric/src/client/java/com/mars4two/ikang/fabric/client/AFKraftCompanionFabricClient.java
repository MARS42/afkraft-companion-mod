package com.mars4two.ikang.fabric.client;

import com.mars4two.ikang.client.AFKraftCompanionClient;
import com.mars4two.ikang.fabric.client.platform.FabricClientNetworkHook;
import com.mars4two.ikang.fabric.platform.FabricNetworkBridge;
import net.fabricmc.api.ClientModInitializer;

public final class AFKraftCompanionFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricNetworkBridge.installClientHook(new FabricClientNetworkHook());
        AFKraftCompanionClient.init();
    }
}
