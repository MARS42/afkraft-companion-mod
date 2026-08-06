package com.mars4two.ikang.client.core.auth;

import com.google.gson.Gson;
import com.mars4two.ikang.client.core.auth.dtos.ModCredentials;
import com.mars4two.ikang.client.core.auth.dtos.ModCredentialsResponse;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

/**
 * Stores the mod credentials obtained from the device authorization flow.
 * Persists them as JSON under the config directory and keeps an in-memory cache.
 */
final class CredentialStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("AFKraft-CredentialStore");
    private static final Gson GSON = new Gson();
    private static final Path CREDENTIALS_FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("afkraft").resolve("credentials.json");

    private static final int ACCESS_TOKEN_EXPIRATION_THRESHOLD_SECONDS = 30;

    private static volatile String accessToken;
    private static volatile long accessTokenExpiresAt;

    private static volatile ModCredentials cachedCredentials;

    private CredentialStore() {
        // Utility class, no instances allowed
    }

    public static synchronized void save(ModCredentialsResponse credentialsResponse) {
        try {
            LOGGER.info("Saving mod credentials...");
            ModCredentials credentials = new ModCredentials(
                credentialsResponse.refresh_expires_in(),
                credentialsResponse.refresh_token(),
                credentialsResponse.token_type(),
                credentialsResponse.scope()
            );

            Files.createDirectories(CREDENTIALS_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(CREDENTIALS_FILE)) {
                GSON.toJson(credentials, writer);
            }
            restrictToOwner(CREDENTIALS_FILE);
            accessToken = credentialsResponse.access_token();
            accessTokenExpiresAt = System.currentTimeMillis() + ((credentialsResponse.expires_in() * 1000) - (ACCESS_TOKEN_EXPIRATION_THRESHOLD_SECONDS * 1000));

            cachedCredentials = credentials;
            LOGGER.info("Mod credentials stored at {}", CREDENTIALS_FILE);
        } catch (IOException e) {
            LOGGER.error("Could not store mod credentials", e);
        }
    }

    public static Optional<ModCredentials> load() {
        ModCredentials current = cachedCredentials;
        if (current != null) {
            return Optional.of(current);
        }
        synchronized (CredentialStore.class) {
            if (cachedCredentials != null) {
                return Optional.of(cachedCredentials);
            }
            if (!Files.exists(CREDENTIALS_FILE)) {
                return Optional.empty();
            }
            try (Reader reader = Files.newBufferedReader(CREDENTIALS_FILE)) {
                cachedCredentials = GSON.fromJson(reader, ModCredentials.class);
                return Optional.ofNullable(cachedCredentials);
            } catch (Exception e) {
                LOGGER.error("Could not read mod credentials", e);
                return Optional.empty();
            }
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

    public static synchronized void clear() {
        cachedCredentials = null;
        try {
            Files.deleteIfExists(CREDENTIALS_FILE);
        } catch (IOException e) {
            LOGGER.error("Could not delete mod credentials", e);
        }
    }

    private static void restrictToOwner(Path file) {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException e) {
            // Windows/NTFS does not support POSIX permissions; the file keeps its default ACLs
            LOGGER.debug("POSIX permissions not applied to {}: {}", file, e.toString());
        }
    }
}
