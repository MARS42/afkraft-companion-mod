package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.auth.dtos.ConnectResponse;
import com.mars4two.ikang.client.core.events.ClientEvents;
import com.mars4two.ikang.client.core.screens.QRModalScreen;
import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.auth.AuthenticationException;
import com.mars4two.ikang.core.auth.dtos.HasConnectedResponse;
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
        // Utility class, no instances allowed
    }

    public static int executeConnect(Consumer<String> onFeedback, Consumer<String> onFeedbackError) {
        Minecraft minecraft = Minecraft.getInstance();

        CompletableFuture.runAsync(() -> {
            try {
                UUID profileId = Minecraft.getInstance().getUser().getProfileId();

                var isConnected = AuthService.getIsConnected(profileId);
                if (isConnected) {
                    minecraft.execute(() -> onFeedback.accept("Ya te encuentras conectado a AFKraft Companion"));
                    return;
                }

                minecraft.execute(() -> onFeedback.accept("Conectando a AFKraft Companion..."));

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
                            notifyUser(minecraft, "El código de vinculación expiró. Ejecuta /afkraft connect de nuevo.");
                            closeModal(minecraft, screen);
                        },
                        (HasConnectedResponse hasConnected) -> {
                            notifyUser(minecraft, "Cuenta vinculada con AFKraft correctamente");
                            closeModal(minecraft, screen);
                            ClientEvents.onClientHasConnected(profileId, hasConnected.otp());
                        }
                );

                screen.setOnClosed(cancelPollHandle);
                screen.setOnCancel(() -> ClientConnectDomainService.cancelConnect(
                        connectData.verificationUrl(),
                        () -> minecraft.execute(() -> onFeedback.accept("Vinculación cancelada")),
                        () -> minecraft.execute(() -> onFeedbackError.accept("Ocurrió un error al cancelar la vinculación"))
                ));

                minecraft.execute(() -> {
                    onFeedback.accept("Esperando aprobación de vinculación...");
                    minecraft.setScreenAndShow(screen);
                });

            } catch (AuthenticationException e) {
                LOGGER.error("[AFKraft] Error de autenticación", e);
                minecraft.execute(() -> onFeedbackError.accept("Error de autenticación: " + e.getMessage()));
            } catch (Exception e) {
                LOGGER.error("[AFKraft] Error inesperado durante la autenticación", e);
                minecraft.execute(() -> onFeedbackError.accept("Error al conectar: " + e.getMessage()));
            }
        });

        return 1;
    }

    private static void closeModal(Minecraft minecraft, QRModalScreen screen) {
        minecraft.execute(() -> {
            if (minecraft.gui.screen() == screen) {
                screen.onClose();
            }
        });
    }

    private static void notifyUser(Minecraft minecraft, String message) {
        minecraft.execute(() -> minecraft.showDebugChat(Component.literal(message)));
    }
}
