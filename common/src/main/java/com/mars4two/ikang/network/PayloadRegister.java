package com.mars4two.ikang.network;

import com.mars4two.ikang.core.events.ServerEvents;
import com.mars4two.ikang.core.network.payloads.client.AFKraftStatusClientBoundPayload;
import com.mars4two.ikang.core.network.payloads.client.AuthorizeServerRequestClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsStatusClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.IsServerAuthenticatedResponse;
import com.mars4two.ikang.core.network.payloads.client.SaveSnapshotPreferencesResultClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.SnapshotPreferencesSyncClientboundPayload;
import com.mars4two.ikang.core.network.payloads.server.AuthorizeServerResponseServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.IsServerAuthenticatedRequest;
import com.mars4two.ikang.core.network.payloads.server.RegisterAFKContainerServerPayload;
import com.mars4two.ikang.core.network.payloads.server.RequestSnapshotPreferencesServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.UpdateSnapshotPreferencesServerboundPayload;
import com.mars4two.ikang.platform.services.Services;

public final class PayloadRegister {

    private PayloadRegister() {
    }

    public static void registerPayloads() {
        registerClientboundPayloads();
        registerServerboundPayloads();
    }

    public static void registerClientboundPayloads() {
        Services.NETWORK.registerClientbound(
                GetCredentialsStatusClientboundPayload.TYPE,
                GetCredentialsStatusClientboundPayload.STREAM_CODEC
        );
        Services.NETWORK.registerClientbound(
                AFKraftStatusClientBoundPayload.TYPE,
                AFKraftStatusClientBoundPayload.STREAM_CODEC
        );
        Services.NETWORK.registerClientbound(
                AuthorizeServerRequestClientboundPayload.TYPE,
                AuthorizeServerRequestClientboundPayload.STREAM_CODEC
        );
        Services.NETWORK.registerClientbound(
                IsServerAuthenticatedResponse.TYPE,
                IsServerAuthenticatedResponse.STREAM_CODEC
        );
        Services.NETWORK.registerClientbound(
                SnapshotPreferencesSyncClientboundPayload.TYPE,
                SnapshotPreferencesSyncClientboundPayload.STREAM_CODEC
        );
        Services.NETWORK.registerClientbound(
                SaveSnapshotPreferencesResultClientboundPayload.TYPE,
                SaveSnapshotPreferencesResultClientboundPayload.STREAM_CODEC
        );
    }

    public static void registerServerboundPayloads() {
        Services.NETWORK.registerServerbound(
                RegisterAFKContainerServerPayload.PAYLOAD_TYPE,
                RegisterAFKContainerServerPayload.STREAM_CODEC,
                (payload, server, player) -> ServerEvents.onRegisterAfkContainer(server, player, payload)
        );
        Services.NETWORK.registerServerbound(
                GetCredentialsRequestServerboundPayload.TYPE,
                GetCredentialsRequestServerboundPayload.STREAM_CODEC,
                (payload, server, player) -> ServerEvents.onGetCredentialsRequest(player, payload)
        );
        Services.NETWORK.registerServerbound(
                AuthorizeServerResponseServerboundPayload.TYPE,
                AuthorizeServerResponseServerboundPayload.STREAM_CODEC,
                (payload, server, player) -> ServerEvents.onServerAuthorizedByPlayer(server, player, payload)
        );
        Services.NETWORK.registerServerbound(
                IsServerAuthenticatedRequest.TYPE,
                IsServerAuthenticatedRequest.STREAM_CODEC,
                (payload, server, player) -> ServerEvents.onIsServerAuthenticatedRequest(player, payload)
        );
        Services.NETWORK.registerServerbound(
                RequestSnapshotPreferencesServerboundPayload.TYPE,
                RequestSnapshotPreferencesServerboundPayload.STREAM_CODEC,
                (payload, server, player) -> ServerEvents.onRequestSnapshotPreferences(server, player, payload)
        );
        Services.NETWORK.registerServerbound(
                UpdateSnapshotPreferencesServerboundPayload.TYPE,
                UpdateSnapshotPreferencesServerboundPayload.STREAM_CODEC,
                (payload, server, player) -> ServerEvents.onUpdateSnapshotPreferences(server, player, payload)
        );
    }
}
