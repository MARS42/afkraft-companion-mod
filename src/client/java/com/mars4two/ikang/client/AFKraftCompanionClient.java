package com.mars4two.ikang.client;

import com.mars4two.ikang.client.command.AFKraftClientConnectCommand;
import com.mars4two.ikang.client.core.events.ClientEvents;
import com.mars4two.ikang.client.core.worker.AFKraftWorker;
import com.mars4two.ikang.core.network.payloads.client.AFKraftStatusClientBoundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsRequestClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsStatusClientboundPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;


public class AFKraftCompanionClient extends AFKraftWorker implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Registrar comandos client-side
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            AFKraftClientConnectCommand.register(dispatcher);
        });

        ClientPlayNetworking.registerGlobalReceiver(GetCredentialsStatusClientboundPayload.TYPE, (payload, context) -> {
            ClientEvents.onGetCredentialsStatus(payload);
        });


        ClientPlayNetworking.registerGlobalReceiver(GetCredentialsRequestClientboundPayload.TYPE, (payload, context) -> {

        });

        ClientPlayNetworking.registerGlobalReceiver(AFKraftStatusClientBoundPayload.TYPE,
                (afkraftStatus, context) ->
                        ClientEvents.onGetAfkraftStatus(afkraftStatus)
        );

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ClientEvents.onAfterScreenInit(screen);
        });
    }
}