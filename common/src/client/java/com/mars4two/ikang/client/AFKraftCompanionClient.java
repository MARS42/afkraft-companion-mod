package com.mars4two.ikang.client;

import com.mars4two.ikang.client.command.AFKraftClientConfigCommand;
import com.mars4two.ikang.client.command.AFKraftClientConnectCommand;
import com.mars4two.ikang.client.command.AFKraftClientEnableCommand;
import com.mars4two.ikang.client.core.events.ClientEvents;
import com.mars4two.ikang.client.core.preferences.ClientContainerButtonConfigService;
import com.mars4two.ikang.client.core.preferences.ClientPreferencesService;
import com.mars4two.ikang.client.core.update.ClientUpdateNotificationManager;
import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.client.platform.services.ClientCommandBridge;
import com.mars4two.ikang.client.platform.services.ClientServices;
import com.mars4two.ikang.core.network.payloads.client.AFKraftStatusClientBoundPayload;
import com.mars4two.ikang.core.network.payloads.client.AuthorizeServerRequestClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsStatusClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.IsServerAuthenticatedResponse;
import com.mars4two.ikang.core.network.payloads.client.SaveSnapshotPreferencesResultClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.SnapshotPreferencesSyncClientboundPayload;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import com.mars4two.ikang.platform.services.Services;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.KeyMapping;

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

        Services.NETWORK.registerClientReceiver(
                SnapshotPreferencesSyncClientboundPayload.TYPE,
                ClientPreferencesService::onPreferencesSync
        );

        Services.NETWORK.registerClientReceiver(
                SaveSnapshotPreferencesResultClientboundPayload.TYPE,
                ClientPreferencesService::onSaveResult
        );

        KeyMapping openConfigKey = ClientUiAdapter.createKeyMapping(
                AFKraftTranslations.KEY_OPEN_CONFIG,
                InputConstants.KEY_0
        );
        ClientServices.KEY_MAPPINGS.registerKeyMapping(
                openConfigKey,
                minecraft -> ClientPreferencesService.requestOpenPreferencesScreen()
        );

        KeyMapping openButtonPosKey = ClientUiAdapter.createKeyMapping(
                AFKraftTranslations.KEY_OPEN_BUTTON_POSITION,
                InputConstants.KEY_MINUS
        );
        ClientServices.KEY_MAPPINGS.registerKeyMapping(
                openButtonPosKey,
                minecraft -> ClientContainerButtonConfigService.openPositionConfigScreen()
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
        AFKraftClientConfigCommand.register(dispatcher, sourceAccess);
    }
}
