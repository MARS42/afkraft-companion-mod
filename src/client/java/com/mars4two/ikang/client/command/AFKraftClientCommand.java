package com.mars4two.ikang.client.command;

import com.google.gson.Gson;
import com.mars4two.ikang.client.auth.CredentialStore;
import com.mars4two.ikang.client.screens.QRModalScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AFKraftClientCommand {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("AFKraft");

    //private static final String BACKEND_URL = "http://localhost:4242/core/auth";
    private static final String BACKEND_URL = "https://api-mvp.afkraft.com/core";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private static final Gson GSON = new Gson();

    private static final ScheduledExecutorService POLLING_EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "afkraft-auth-polling");
        thread.setDaemon(true);
        return thread;
    });

    private static final AtomicReference<AtomicBoolean> ACTIVE_POLL_CANCEL = new AtomicReference<>();

    private AFKraftClientCommand() {
        // Command class, no instances allowed
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            LiteralArgumentBuilder.<FabricClientCommandSource>literal("afkraft")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("connect")
                    .executes(AFKraftClientCommand::executeConnect))
        );
    }
    
    private static int executeConnect(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        Minecraft minecraft = Minecraft.getInstance();
        
        if (minecraft.player == null) {
            source.sendError(Component.literal("Debes estar en un mundo para usar este comando"));
            return 0;
        }
        
        source.sendFeedback(Component.literal("Conectando a AFKraft Companion..."));
        
        var sessionService = minecraft.services().sessionService();
        var user = minecraft.getUser();
        UUID profileId = user.getProfileId();
//        UUID profileId = UUID.fromString("2c349e41-d68a-49e3-9385-111bb484ed32");
        String username = user.getName();
        
        LOGGER.info("[AFKraft] Iniciando autenticación para usuario: {} ({})", username, profileId);

        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.debug("[AFKraft] Paso 1: Solicitando serverId al backend...");
                String serverId = requestServerId(profileId);
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
                    source.sendFeedback(Component.literal("Esperando aprobación de vinculación..."));
                    minecraft.setScreenAndShow(screen);
                });
                
            } catch (AuthenticationException e) {
                LOGGER.error("[AFKraft] Error de autenticación", e);
                minecraft.execute(() -> 
                    source.sendError(Component.literal("Error de autenticación: " + e.getMessage()))
                );
            } catch (Exception e) {
                LOGGER.error("[AFKraft] Error inesperado durante la autenticación", e);
                minecraft.execute(() -> 
                    source.sendError(Component.literal("Error al conectar: " + e.getMessage()))
                );
            }
        });
        
        return 1;
    }

    private static String requestServerId(UUID profileId) throws AuthenticationException {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
            
            String jsonBody = String.format("{\"profileId\":\"%s\"}", profileId.toString());
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_URL + "/mod-auth/get-server"))
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new AuthenticationException("No se pudo obtener serverId (HTTP " + response.statusCode() + ")");
            }

            String responseBody = response.body();
            int start = responseBody.indexOf("\"serverId\":\"") + 12;
            int end = responseBody.indexOf("\"", start);
            
            if (start < 12 || end < 0) {
                throw new AuthenticationException("Respuesta inválida del servidor");
            }
            
            return responseBody.substring(start, end);
            
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("Error de conexión con el backend: " + e.getMessage());
        }
    }
    
    private static HasJoinedResponse validateHasJoined(String username, String serverId, UUID profileId)
            throws AuthenticationException {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();

            String jsonBody = String.format(
                "{\"name\":\"%s\",\"serverId\":\"%s\",\"profileId\":\"%s\"}",
                username, serverId, profileId.toString()
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_URL + "/mod-auth/has-joined"))
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new AuthenticationException("Validación fallida: credenciales inválidas");
            }

            if (response.statusCode() != 200) {
                throw new AuthenticationException("Error en validación (HTTP " + response.statusCode() + ")");
            }

            HasJoinedResponse dto = GSON.fromJson(response.body(), HasJoinedResponse.class);

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

    private static void cancelActivePolling() {
        AtomicBoolean previous = ACTIVE_POLL_CANCEL.getAndSet(null);
        if (previous != null) {
            previous.set(true);
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


    private static Optional<ModCredentialsResponse> validateHasConnected(String deviceCode) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(REQUEST_TIMEOUT)
                    .build();

            String jsonBody = String.format("{\"deviceCode\":\"%s\"}", deviceCode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_URL + "/mod-auth/has-connected"))
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() != 200) {
                LOGGER.warn("Has connected is not success (HTTP {}, Body: {}), retrying...", response.statusCode(), body);
                return Optional.empty();
            }

            ModCredentialsResponse dto = GSON.fromJson(body, ModCredentialsResponse.class);
            return Optional.ofNullable(dto);

        }  catch (Exception e) {
            LOGGER.error("Unexpected error during hasConnected validation: ", e);
            return Optional.empty();
        }
    }
    
    private static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
