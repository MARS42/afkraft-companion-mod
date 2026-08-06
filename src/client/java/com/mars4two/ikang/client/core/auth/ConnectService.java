package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.auth.dtos.*;
import com.mars4two.ikang.client.core.config.GsonConfig;
import com.mars4two.ikang.client.core.config.HttpConfig;
import com.mars4two.ikang.client.core.screens.QRModalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ConnectService {

    private static final Logger LOGGER = LoggerFactory.getLogger("afkraft-connect-service");

    private static final HttpClient client = HttpConfig.getHttpClient();

    private static final AtomicReference<AtomicBoolean> ACTIVE_POLL_CANCEL = new AtomicReference<>();
    private static final ScheduledExecutorService POLLING_EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "afkraft-auth-polling");
        thread.setDaemon(true);
        return thread;
    });

    private ConnectService() {
        // Utility class, no instances allowed
    }

    public static int executeConnect(Consumer<String> onFeedback, Consumer<String> onFeedbackError) {
        Minecraft minecraft = Minecraft.getInstance();

        onFeedback.accept("Conectando a AFKraft Companion...");

        var sessionService = minecraft.services().sessionService();
        var user = minecraft.getUser();
        UUID profileId = user.getProfileId();
//        UUID profileId = UUID.fromString("2c349e41-d68a-49e3-9385-111bb484ed32");
        String username = user.getName();

        LOGGER.info("[AFKraft] Iniciando autenticación para usuario: {} ({})", username, profileId);

        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.debug("[AFKraft] Paso 1: Solicitando serverId al backend...");
                String serverId = getServerId(profileId);
//                String serverId = "a7830bd9-3a7d-4792-a9cc-00607520106f";
                LOGGER.info("[AFKraft] serverId obtenido: {}", serverId);

                LOGGER.debug("[AFKraft] Paso 2: Realizando joinServer con Mojang...");
                sessionService.joinServer(profileId, user.getAccessToken(), serverId);  // Comment in local dev
                LOGGER.info("[AFKraft] joinServer completado exitosamente");

                LOGGER.debug("[AFKraft] Paso 3: Validando con hasJoined en backend...");
                HasJoinedResponse hasJoined = validateHasJoined(username, serverId, profileId);       // Comment in local dev
                LOGGER.info("[AFKraft] Validación hasJoined exitosa");

                String connectUrl = hasJoined.verificationUrl();

                var screen = new QRModalScreen(connectUrl);
                validateHasConnected(hasJoined.deviceCode(), hasJoined.interval(), hasJoined.expiresIn(), screen);

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

    private static String getServerId(UUID profileId) throws AuthenticationException {
        try {
            String jsonBody = GsonConfig.toJson(new GetServerIdRequest(profileId));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(HttpConfig.getServerIdUri())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AuthenticationException("No se pudo obtener serverId (HTTP " + response.statusCode() + ")");
            }

            GetServerIdResponse responseDto = GsonConfig.fromJson(response.body(), GetServerIdResponse.class);
            return responseDto.serverId();

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("Error de conexión con el backend: " + e.getMessage());
        }
    }

    private static HasJoinedResponse validateHasJoined(String username, String serverId, UUID profileId)
            throws AuthenticationException {
        try {
            String jsonBody = GsonConfig.toJson(new HasJoinedRequest(username, serverId, profileId));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(HttpConfig.getHasJoinedUri())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new AuthenticationException("Validación fallida: credenciales inválidas");
            }

            if (response.statusCode() != 200) {
                throw new AuthenticationException("Error en validación (HTTP " + response.statusCode() + ")");
            }

            HasJoinedResponse dto = GsonConfig.fromJson(response.body(), HasJoinedResponse.class);

            if (dto == null || dto.verificationUrl() == null || dto.deviceCode() == null) {
                throw new AuthenticationException("Invalid server response");
            }

            return dto;

        }  catch (Exception e) {
            LOGGER.error("Unexpected error during hasJoined validation: ", e);
            throw new AuthenticationException(e.getMessage());
        }
    }

    private static void validateHasConnected(String deviceCode, int intervalInSeconds, int expiresInSeconds, QRModalScreen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        int interval = Math.max(1, intervalInSeconds);
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(expiresInSeconds);

        cancelActivePolling();

        AtomicBoolean cancelled = new AtomicBoolean(false);
        ACTIVE_POLL_CANCEL.set(cancelled);
        screen.setOnClosed(() -> cancelled.set(true));

        LOGGER.info("[AFKraft] Iniciando polling de vinculación (cada {}s, expira en {}s)", interval, expiresInSeconds);
        schedulePoll(deviceCode, interval, deadlineNanos, screen, minecraft, cancelled);
    }

    private static void cancelActivePolling() {
        AtomicBoolean previous = ACTIVE_POLL_CANCEL.getAndSet(null);
        if (previous != null) {
            previous.set(true);
        }
    }

    private static void schedulePoll(String deviceCode, int interval, long deadlineNanos,
                                     QRModalScreen screen, Minecraft minecraft, AtomicBoolean cancelled) {
        POLLING_EXECUTOR.schedule(() -> {
            if (cancelled.get()) {
                LOGGER.info("[AFKraft] Polling de vinculación cancelado");
                return;
            }

            if (System.nanoTime() >= deadlineNanos) {
                LOGGER.warn("[AFKraft] El código de vinculación expiró");
                notifyUser(minecraft, "El código de vinculación expiró. Ejecuta /afkraft connect de nuevo.");
                closeModal(minecraft, screen);
                return;
            }

            Optional<ModCredentialsResponse> credentials = validateHasConnected(deviceCode)
                    .filter(dto -> dto.access_token() != null && !dto.access_token().isBlank());

            if (credentials.isPresent()) {
                CredentialStore.save(credentials.get());
                LOGGER.info("[AFKraft] Vinculación completada, credenciales del mod guardadas");
                notifyUser(minecraft, "Cuenta vinculada con AFKraft correctamente");
                closeModal(minecraft, screen);
                return;
            }

            schedulePoll(deviceCode, interval, deadlineNanos, screen, minecraft, cancelled);
        }, interval, TimeUnit.SECONDS);
    }

    private static Optional<ModCredentialsResponse> validateHasConnected(String deviceCode) {
        try {
            String jsonBody = GsonConfig.toJson(new HasConnectedRequest(deviceCode));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(HttpConfig.getHasConnectedUri())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() != 200) {
                LOGGER.warn("Has connected is not success (HTTP {}, Body: {}), retrying...", response.statusCode(), body);
                return Optional.empty();
            }

            ModCredentialsResponse dto = GsonConfig.fromJson(body, ModCredentialsResponse.class);
            return Optional.ofNullable(dto);

        }  catch (Exception e) {
            LOGGER.error("Unexpected error during hasConnected validation: ", e);
            return Optional.empty();
        }
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
