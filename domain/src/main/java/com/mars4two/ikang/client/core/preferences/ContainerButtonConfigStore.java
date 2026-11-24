package com.mars4two.ikang.client.core.preferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Local JSON persistence store for {@link ContainerButtonPosition}.
 */
public final class ContainerButtonConfigStore {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerButtonConfigStore.class);
    private static final int SCHEMA_VERSION = 1;
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;

    public ContainerButtonConfigStore(Path configFile) {
        this.configFile = Objects.requireNonNull(configFile, "configFile");
    }

    public Path configFile() {
        return this.configFile;
    }

    public ContainerButtonPosition load() {
        if (!Files.isRegularFile(this.configFile)) {
            return ContainerButtonPosition.DEFAULT;
        }

        try {
            String rawJson = Files.readString(this.configFile, StandardCharsets.UTF_8);
            if (rawJson == null || rawJson.isBlank()) {
                return ContainerButtonPosition.DEFAULT;
            }

            PersistedConfig parsed = PRETTY_GSON.fromJson(rawJson, PersistedConfig.class);
            if (parsed == null || parsed.offsetX == null || parsed.offsetY == null) {
                LOG.warn("Client button config at {} is incomplete; falling back to default position", this.configFile);
                return ContainerButtonPosition.DEFAULT;
            }

            return new ContainerButtonPosition(parsed.offsetX, parsed.offsetY);
        } catch (IOException | JsonSyntaxException e) {
            LOG.warn("Failed to load client button config from {}; falling back to default position", this.configFile, e);
            return ContainerButtonPosition.DEFAULT;
        }
    }

    public void save(ContainerButtonPosition position) throws IOException {
        Objects.requireNonNull(position, "position");

        Path parent = this.configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        PersistedConfig payload = new PersistedConfig(SCHEMA_VERSION, position.offsetX(), position.offsetY());
        String json = PRETTY_GSON.toJson(payload) + System.lineSeparator();

        Path tempFile = this.configFile.resolveSibling(this.configFile.getFileName().toString() + ".tmp");
        Files.writeString(tempFile, json, StandardCharsets.UTF_8);
        try {
            Files.move(tempFile, this.configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, this.configFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final class PersistedConfig {
        final int version;
        final Integer offsetX;
        final Integer offsetY;

        private PersistedConfig(int version, Integer offsetX, Integer offsetY) {
            this.version = version;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }
}
