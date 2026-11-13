package com.mars4two.ikang.client;

import com.mars4two.ikang.client.command.AFKraftClientConnectCommand;
import com.mars4two.ikang.client.command.AFKraftClientEnableCommand;
import com.mars4two.ikang.client.core.events.ClientEvents;
import com.mars4two.ikang.client.core.update.ClientUpdateNotificationManager;
import com.mars4two.ikang.client.platform.services.ClientCommandBridge;
import com.mars4two.ikang.client.platform.services.ClientServices;
import com.mars4two.ikang.core.network.payloads.client.AFKraftStatusClientBoundPayload;
import com.mars4two.ikang.core.network.payloads.client.AuthorizeServerRequestClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsStatusClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.IsServerAuthenticatedResponse;
import com.mars4two.ikang.platform.services.Services;
import com.mojang.brigadier.CommandDispatcher;

public final class AFKraftCompanionClient {

    private AFKraftCompanionClient() {
    }

    public static void init() {
        ClientServices.COMMANDS.registerCommands(AFKraftCompanionClient::registerClientCommands);

        Services.NETWORK.registerClientReceiver(
                GetCredentialsStatusClientboundPayload.TYPE,
                (payload, player) -> ClientEvents.onGetCredentialsStatus(payload)
        );

        Services.NETWORK.registerClientReceiver(
                AuthorizeServerRequestClientboundPayload.TYPE,
                ClientEvents::onAuthorizeServerRequest
        );

        Services.NETWORK.registerClientReceiver(
                IsServerAuthenticatedResponse.TYPE,
                (payload, player) -> AFKraftClientEnableCommand.onIsServerAuthenticatedResponse(player, payload)
        );

        Services.NETWORK.registerClientReceiver(
                AFKraftStatusClientBoundPayload.TYPE,
                (payload, player) -> ClientEvents.onGetAfkraftStatus(payload)
        );

        ClientServices.SCREENS.onAfterScreenInit(ClientEvents::onAfterScreenInit);

        ClientUpdateNotificationManager.init();
    }

    private static <S> void registerClientCommands(
            CommandDispatcher<S> dispatcher,
            ClientCommandBridge.SourceAccess<S> sourceAccess
    ) {
        AFKraftClientConnectCommand.register(dispatcher, sourceAccess);
        AFKraftClientEnableCommand.register(dispatcher, sourceAccess);
    }
}
