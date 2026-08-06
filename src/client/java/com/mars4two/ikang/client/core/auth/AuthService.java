package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.auth.dtos.*;
import com.mars4two.ikang.client.core.config.GsonConfig;
import com.mars4two.ikang.client.core.config.HttpConfig;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsStatusServerboundPayload;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger("afkraft-auth-service");
    private static final HttpClient client = HttpConfig.getHttpClient();

    private AuthService() {
        // Utility class, no instances allowed
    }

    public static void getAccessToken(Consumer<Optional<String>> callback) {
        log.info("Checking authentication...");
        CompletableFuture.runAsync(() -> {
            var modCredentials = CredentialStore.load();
            if(modCredentials.isEmpty()) {
                log.info("No mod credentials found");
                callback.accept(Optional.empty());
                return;
            }

            var currentAccessToken = CredentialStore.getAccessToken();
            if(currentAccessToken.isPresent()) {
                log.info("Access token is present");
                callback.accept(currentAccessToken);
                return;
            }

            log.info("Access token is not present, refreshing with mod credentials...");
            var refreshedCredentials = refreshAccessToken(modCredentials.get());

            if(refreshedCredentials.isEmpty()) {
                callback.accept(Optional.empty());
                return;
            }

            CredentialStore.save(refreshedCredentials.get());

            log.info("Access token refreshed successfully");
            callback.accept(CredentialStore.getAccessToken());
        });
    }

    private static Optional<ModCredentialsResponse> refreshAccessToken(ModCredentials modCredentials) {
        log.info("Refreshing access token...");

        try {
            String jsonBody = GsonConfig.toJson(new RefreshModCredentialsRequest(modCredentials.refreshToken()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(HttpConfig.getRefreshAccessTokenUri())
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

            ModCredentialsResponse dto = GsonConfig.fromJson(response.body(), ModCredentialsResponse.class);

            return Optional.ofNullable(dto);
        }  catch (Exception e) {
            log.error("Unexpected error during refreshing mod credentials: ", e);
            return Optional.empty();
        }
    }
}
