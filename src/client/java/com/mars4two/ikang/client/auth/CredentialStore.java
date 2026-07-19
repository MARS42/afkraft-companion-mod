package com.mars4two.ikang.client.auth;

import com.google.gson.Gson;
import com.mars4two.ikang.client.command.ModCredentialsResponse;
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
public final class CredentialStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("AFKraft-CredentialStore");
    private static final Gson GSON = new Gson();
    private static final Path CREDENTIALS_FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("afkraft").resolve("credentials.json");

    private static volatile ModCredentialsResponse cached;

    private CredentialStore() {
        // Utility class, no instances allowed
    }

    public static synchronized void save(ModCredentialsResponse credentials) {
        try {
            Files.createDirectories(CREDENTIALS_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(CREDENTIALS_FILE)) {
                GSON.toJson(credentials, writer);
            }
            restrictToOwner(CREDENTIALS_FILE);
            cached = credentials;
            LOGGER.info("Mod credentials stored at {}", CREDENTIALS_FILE);
        } catch (IOException e) {
            LOGGER.error("Could not store mod credentials", e);
        }
    }

    public static Optional<ModCredentialsResponse> load() {
        ModCredentialsResponse current = cached;
        if (current != null) {
            return Optional.of(current);
        }
        synchronized (CredentialStore.class) {
            if (cached != null) {
                return Optional.of(cached);
            }
            if (!Files.exists(CREDENTIALS_FILE)) {
                return Optional.empty();
            }
            try (Reader reader = Files.newBufferedReader(CREDENTIALS_FILE)) {
                cached = GSON.fromJson(reader, ModCredentialsResponse.class);
                return Optional.ofNullable(cached);
            } catch (Exception e) {
                LOGGER.error("Could not read mod credentials", e);
                return Optional.empty();
            }
        }
    }

    public static synchronized void clear() {
        cached = null;
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
