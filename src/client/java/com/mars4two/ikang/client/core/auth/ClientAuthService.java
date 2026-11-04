package com.mars4two.ikang.client.core.auth;

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
        // Utility class, no instances allowed
    }

    public static void authenticate(
            BiConsumer<String, String> successCallback,
            Consumer<Exception> errorCallback,
            Runnable notConnectedCallback
    ) {
        var user = Minecraft.getInstance().getUser();
        ClientAuthDomainService.authenticate(
                user.getProfileId(),
                user.getName(),
                user.getAccessToken(),
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
        var user = Minecraft.getInstance().getUser();
        ClientAuthDomainService.authorizeServer(
                user.getProfileId(),
                user.getName(),
                user.getAccessToken(),
                ClientAuthService::joinMojangSession,
                successCallback,
                errorCallback,
                notConnectedCallback
        );
    }

    public static ClientAuthDomainService.HasJoinedResult joinServer() throws AuthenticationException {
        var user = Minecraft.getInstance().getUser();
        return ClientAuthDomainService.joinServer(
                user.getProfileId(),
                user.getName(),
                user.getAccessToken(),
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
        try {
            Minecraft.getInstance().services().sessionService().joinServer(profileId, accessToken, serverId);
        } catch (com.mojang.authlib.exceptions.AuthenticationException e) {
            throw new AuthenticationException(e.getMessage(), e);
        }
    }
}
