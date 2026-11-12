package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.auth.dtos.ConnectResponse;
import com.mars4two.ikang.client.core.events.ClientEvents;
import com.mars4two.ikang.client.core.screens.QRModalScreen;
import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.auth.AuthenticationException;
import com.mars4two.ikang.core.auth.dtos.HasConnectedResponse;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Minecraft client UI adapter orchestrating {@link QRModalScreen} and main-thread notifications
 * around {@link ClientConnectDomainService}.
 */
public final class ClientConnectService {

    private static final Logger LOGGER = LoggerFactory.getLogger("afkraft-connect-service");

    private ClientConnectService() {
    }

    public static int executeConnect(Consumer<Component> onFeedback, Consumer<Component> onFeedbackError) {
        Minecraft minecraft = Minecraft.getInstance();

        CompletableFuture.runAsync(() -> {
            try {
                UUID profileId = ClientUiAdapter.clientProfileId(minecraft);

                var isConnected = AuthService.getIsConnected(profileId);
                if (isConnected) {
                    minecraft.execute(() -> onFeedback.accept(
                            AFKraftTranslations.translatable(AFKraftTranslations.CMD_CONNECT_ALREADY)
                    ));
                    return;
                }

                minecraft.execute(() -> onFeedback.accept(
                        AFKraftTranslations.translatable(AFKraftTranslations.CMD_CONNECT_CONNECTING)
                ));

                LOGGER.info("[AFKraft] Iniciando autenticación para usuario: {}", profileId);

                var joinServerResult = ClientAuthService.joinServer();

                LOGGER.debug("[AFKraft] Paso 4: Realizando connect con AFKraft...");
                ConnectResponse connectData = ClientConnectDomainService.connect(
                        joinServerResult.serverId(),
                        profileId,
                        joinServerResult.hasJoinedResponse()
                );
                LOGGER.info("[AFKraft] connect completado exitosamente");

                String connectUrl = connectData.verificationUrl();

                var screen = new QRModalScreen(connectUrl);
                Runnable cancelPollHandle = ClientConnectDomainService.startPolling(
                        connectData,
                        () -> {
                            notifyUser(minecraft, AFKraftTranslations.translatable(AFKraftTranslations.CMD_CONNECT_EXPIRED));
                            closeModal(minecraft, screen);
                        },
                        (HasConnectedResponse hasConnected) -> {
                            notifyUser(minecraft, AFKraftTranslations.translatable(AFKraftTranslations.CMD_CONNECT_SUCCESS));
                            closeModal(minecraft, screen);
                            ClientEvents.onClientHasConnected(profileId, hasConnected.otp());
                        }
                );

                screen.setOnClosed(cancelPollHandle);
                screen.setOnCancel(() -> ClientConnectDomainService.cancelConnect(
                        connectData.verificationUrl(),
                        () -> minecraft.execute(() -> onFeedback.accept(
                                AFKraftTranslations.translatable(AFKraftTranslations.CMD_CONNECT_CANCELLED)
                        )),
                        () -> minecraft.execute(() -> onFeedbackError.accept(
                                AFKraftTranslations.translatable(AFKraftTranslations.CMD_CONNECT_CANCEL_ERROR)
                        ))
                ));

                minecraft.execute(() -> {
                    onFeedback.accept(AFKraftTranslations.translatable(AFKraftTranslations.CMD_CONNECT_WAITING));
                    ClientUiAdapter.showScreen(minecraft, screen);
                });

            } catch (AuthenticationException e) {
                LOGGER.error("[AFKraft] Error de autenticación", e);
                minecraft.execute(() -> onFeedbackError.accept(
                        AFKraftTranslations.translatable(AFKraftTranslations.CMD_CONNECT_AUTH_ERROR, e.getMessage())
                ));
            } catch (Exception e) {
                LOGGER.error("[AFKraft] Error inesperado durante la autenticación", e);
                minecraft.execute(() -> onFeedbackError.accept(
                        AFKraftTranslations.translatable(AFKraftTranslations.CMD_CONNECT_GENERIC_ERROR, e.getMessage())
                ));
            }
        });

        return 1;
    }

    private static void closeModal(Minecraft minecraft, QRModalScreen screen) {
        minecraft.execute(() -> {
            if (ClientUiAdapter.isCurrentScreen(minecraft, screen)) {
                screen.onClose();
            }
        });
    }

    private static void notifyUser(Minecraft minecraft, Component message) {
        minecraft.execute(() -> ClientUiAdapter.notifyClientChat(minecraft, message));
    }
}
