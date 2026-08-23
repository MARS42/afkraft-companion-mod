package com.mars4two.ikang.core.auth;

import com.mars4two.ikang.core.auth.dtos.ModCredentials;
import com.mars4two.ikang.core.auth.dtos.ModCredentialsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Stores the mod credentials obtained from the device authorization flow.
 * Persists them as JSON under the config directory and keeps an in-memory cache.
 */
final class CredentialStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("AFKraft-CredentialStore");

    private static final int ACCESS_TOKEN_EXPIRATION_THRESHOLD_SECONDS = 30;

    private static volatile String accessToken;
    private static volatile long accessTokenExpiresAt;

    private static volatile ModCredentials cachedCredentials;

    private CredentialStore() {
        // Utility class, no instances allowed
    }

    public static synchronized void save(ModCredentialsResponse credentialsResponse) {
        LOGGER.info("Saving mod credentials...");
        ModCredentials credentials = new ModCredentials(
                credentialsResponse.refresh_expires_in(),
                credentialsResponse.refresh_token(),
                credentialsResponse.token_type(),
                credentialsResponse.scope()
        );

        accessToken = credentialsResponse.access_token();
        accessTokenExpiresAt = System.currentTimeMillis() + ((credentialsResponse.expires_in() * 1000) - (ACCESS_TOKEN_EXPIRATION_THRESHOLD_SECONDS * 1000));

        cachedCredentials = credentials;
        LOGGER.info("Mod credentials stored in memory");
    }

    public static Optional<ModCredentials> load() {
        ModCredentials current = cachedCredentials;
        if (current != null) {
            LOGGER.debug("Mod credentials loaded from memory");
            return Optional.of(current);
        }

        synchronized (CredentialStore.class) {
            if (cachedCredentials != null) {
                LOGGER.debug("Mod credentials loaded from memory");
                return Optional.of(cachedCredentials);
            }
            return Optional.empty();
        }
    }

    public static Optional<String> getAccessToken() {

        if(accessToken == null) {
            return Optional.empty();
        }

        if(System.currentTimeMillis() > accessTokenExpiresAt) {
            LOGGER.info("Access token expired");
            accessToken = null;
            return Optional.empty();
        }

        return Optional.ofNullable(accessToken);
    }
}
