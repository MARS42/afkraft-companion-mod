package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.core.auth.AuthenticationException;
import net.minecraft.client.Minecraft;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Minecraft client session adapter delegating authentication workflows to {@link ClientAuthDomainService}.
 */
public final class ClientAuthService {

    private ClientAuthService() {
    }

    public static void authenticate(
            BiConsumer<String, String> successCallback,
            Consumer<Exception> errorCallback,
            Runnable notConnectedCallback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientAuthDomainService.authenticate(
                ClientUiAdapter.clientProfileId(minecraft),
                ClientUiAdapter.clientUserName(minecraft),
                ClientUiAdapter.clientAccessToken(minecraft),
                ClientAuthService::joinMojangSession,
                successCallback,
                errorCallback,
                notConnectedCallback
        );
    }

    public static void authorizeServer(
            Consumer<ClientAuthDomainService.HasJoinedResult> successCallback,
            Consumer<Exception> errorCallback,
            Runnable notConnectedCallback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientAuthDomainService.authorizeServer(
                ClientUiAdapter.clientProfileId(minecraft),
                ClientUiAdapter.clientUserName(minecraft),
                ClientUiAdapter.clientAccessToken(minecraft),
                ClientAuthService::joinMojangSession,
                successCallback,
                errorCallback,
                notConnectedCallback
        );
    }

    public static ClientAuthDomainService.HasJoinedResult joinServer() throws AuthenticationException {
        Minecraft minecraft = Minecraft.getInstance();
        return ClientAuthDomainService.joinServer(
                ClientUiAdapter.clientProfileId(minecraft),
                ClientUiAdapter.clientUserName(minecraft),
                ClientUiAdapter.clientAccessToken(minecraft),
                ClientAuthService::joinMojangSession
        );
    }

    public static void sendPlayerAuthorization(
            UUID currentServerId,
            UUID profileId,
            ClientAuthDomainService.HasJoinedResult joinedResult,
            String serverName,
            Runnable successCallback,
            Runnable errorCallback
    ) {
        ClientAuthDomainService.sendPlayerAuthorization(
                currentServerId,
                profileId,
                joinedResult,
                serverName,
                successCallback,
                errorCallback
        );
    }

    private static void joinMojangSession(UUID profileId, String accessToken, String serverId)
            throws AuthenticationException {
        ClientUiAdapter.joinMojangSession(Minecraft.getInstance(), profileId, accessToken, serverId);
    }
}
