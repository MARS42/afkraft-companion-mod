package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.auth.dtos.CancelConnectRequest;
import com.mars4two.ikang.client.core.auth.dtos.ConnectRequest;
import com.mars4two.ikang.client.core.auth.dtos.ConnectResponse;
import com.mars4two.ikang.core.auth.AuthenticationException;
import com.mars4two.ikang.core.auth.dtos.HasConnectedRequest;
import com.mars4two.ikang.core.auth.dtos.HasConnectedResponse;
import com.mars4two.ikang.core.auth.dtos.HasJoinedResponse;
import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Pure domain OAuth-style device connection and background polling service.
 */
public final class ClientConnectDomainService {

    private static final Logger LOGGER = LoggerFactory.getLogger("afkraft-connect-service");
    private static final HttpClient client = HttpConfig.getHttpClient();

    private static final AtomicReference<AtomicBoolean> ACTIVE_POLL_CANCEL = new AtomicReference<>();
    private static final ScheduledExecutorService POLLING_EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "afkraft-auth-polling");
        thread.setDaemon(true);
        return thread;
    });

    private ClientConnectDomainService() {
        // Utility class, no instances allowed
    }

    public static ConnectResponse connect(String serverId, UUID profileId, HasJoinedResponse hasJoinedResponse)
            throws AuthenticationException {
        try {
            String jsonBody = GsonConfig.toJson(new ConnectRequest(serverId, profileId, hasJoinedResponse.joinOtp()));

            HttpRequest request = buildJsonPostRequest(HttpConfig.getConnectUri(), jsonBody);

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new AuthenticationException("Validación fallida: credenciales inválidas");
            }

            if (response.statusCode() != 200) {
                throw new AuthenticationException("Error en validación (HTTP " + response.statusCode() + ")");
            }

            ConnectResponse dto = GsonConfig.fromJson(response.body(), ConnectResponse.class);

            if (dto == null || dto.verificationUrl() == null || dto.deviceCode() == null) {
                throw new AuthenticationException("Invalid server response");
            }

            return dto;
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during connect request: ", e);
            throw new AuthenticationException(e.getMessage(), e);
        }
    }

    public static void cancelConnect(String verificationUrl, Runnable successCallback, Runnable errorCallback) {
        cancelActivePolling();
        try {
            String jsonBody = GsonConfig.toJson(new CancelConnectRequest(verificationUrl));

            HttpRequest request = buildJsonPostRequest(HttpConfig.getCancelConnectUri(), jsonBody);

            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            LOGGER.info("[AFKraft] Connection cancelled successfully on backend");
                            successCallback.run();
                        } else {
                            LOGGER.warn("[AFKraft] Failed to cancel connection on backend (HTTP {})", response.statusCode());
                            errorCallback.run();
                        }
                    })
                    .exceptionally(ex -> {
                        LOGGER.error("[AFKraft] Error sending cancel connect request: {}", ex.getMessage());
                        errorCallback.run();
                        return null;
                    });
        } catch (Exception e) {
            LOGGER.error("[AFKraft] Unexpected error preparing cancel connect request: ", e);
        }
    }

    public static Runnable startPolling(
            ConnectResponse connectResponse,
            Runnable onExpired,
            Consumer<HasConnectedResponse> onApproved
    ) {
        long intervalSeconds = Math.max(1, connectResponse.interval());
        LOGGER.info("[AFKraft] Iniciando polling de vinculación (cada {}s, expira en {}s)", intervalSeconds, connectResponse.expiresIn());
        return startPollingWithPoller(
                connectResponse,
                intervalSeconds,
                TimeUnit.SECONDS,
                ClientConnectDomainService::validateHasConnected,
                onExpired,
                onApproved
        );
    }

    public static Runnable startPollingWithPoller(
            ConnectResponse connectResponse,
            long intervalDelay,
            TimeUnit delayUnit,
            Function<String, Optional<HasConnectedResponse>> poller,
            Runnable onExpired,
            Consumer<HasConnectedResponse> onApproved
    ) {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(connectResponse.expiresIn());
        cancelActivePolling();

        AtomicBoolean cancelled = new AtomicBoolean(false);
        ACTIVE_POLL_CANCEL.set(cancelled);

        schedulePoll(
                connectResponse.deviceCode(),
                Math.max(1L, intervalDelay),
                delayUnit,
                deadlineNanos,
                poller,
                cancelled,
                onExpired,
                onApproved
        );

        return () -> cancelled.set(true);
    }

    public static void cancelActivePolling() {
        AtomicBoolean previous = ACTIVE_POLL_CANCEL.getAndSet(null);
        if (previous != null) {
            previous.set(true);
        }
    }

    private static void schedulePoll(
            String deviceCode,
            long intervalDelay,
            TimeUnit delayUnit,
            long deadlineNanos,
            Function<String, Optional<HasConnectedResponse>> poller,
            AtomicBoolean cancelled,
            Runnable onExpired,
            Consumer<HasConnectedResponse> onApproved
    ) {
        POLLING_EXECUTOR.schedule(() -> {
            if (cancelled.get()) {
                LOGGER.info("[AFKraft] Polling de vinculación cancelado");
                return;
            }

            if (System.nanoTime() >= deadlineNanos) {
                LOGGER.warn("[AFKraft] El código de vinculación expiró");
                onExpired.run();
                return;
            }

            Optional<HasConnectedResponse> hasConnected = poller.apply(deviceCode);
            if (hasConnected.isPresent()) {
                LOGGER.info("[AFKraft] Vinculación completada");
                onApproved.accept(hasConnected.get());
                return;
            }

            schedulePoll(deviceCode, intervalDelay, delayUnit, deadlineNanos, poller, cancelled, onExpired, onApproved);
        }, intervalDelay, delayUnit);
    }

    public static Optional<HasConnectedResponse> validateHasConnected(String deviceCode) {
        try {
            String jsonBody = GsonConfig.toJson(new HasConnectedRequest(deviceCode));

            HttpRequest request = buildJsonPostRequest(HttpConfig.getHasConnectedUri(), jsonBody);

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() != 200) {
                LOGGER.warn("Has connected is not success (HTTP {}, Body: {}), retrying...", response.statusCode(), body);
                return Optional.empty();
            }

            HasConnectedResponse dto = GsonConfig.fromJson(response.body(), HasConnectedResponse.class);
            return Optional.ofNullable(dto);
        } catch (Exception e) {
            LOGGER.error("Unexpected error during hasConnected validation: ", e);
            return Optional.empty();
        }
    }

    static HttpRequest buildJsonPostRequest(URI uri, String jsonBody) {
        return HttpConfig.newBackendRequestBuilder(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }
}
