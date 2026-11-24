package com.mars4two.ikang.client.core.preferences;

import com.mars4two.ikang.client.core.screens.ContainerButtonPositionScreen;
import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates local client storage and screen opening for the AFK container button position.
 */
public final class ClientContainerButtonConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientContainerButtonConfigService.class);
    private static final String CONFIG_FILE_NAME = "afkraft_companion-client.json";

    private static final AtomicReference<ContainerButtonConfigStore> STORE = new AtomicReference<>();
    private static final AtomicReference<ContainerButtonPosition> CACHED_POSITION = new AtomicReference<>();

    private ClientContainerButtonConfigService() {
    }

    public static ContainerButtonPosition getPosition() {
        ContainerButtonPosition cached = CACHED_POSITION.get();
        if (cached != null) {
            return cached;
        }

        ContainerButtonPosition loaded = resolveStore().load();
        if (CACHED_POSITION.compareAndSet(null, loaded)) {
            return loaded;
        }
        return CACHED_POSITION.get();
    }

    public static boolean savePosition(ContainerButtonPosition position) {
        Objects.requireNonNull(position, "position");
        try {
            resolveStore().save(position);
            CACHED_POSITION.set(position);
            return true;
        } catch (IOException e) {
            LOG.error("Failed to save container button position to {}", resolveStore().configFile(), e);
            return false;
        }
    }

    public static void openPositionConfigScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        Screen current = ClientUiAdapter.currentScreen(minecraft);
        if (current instanceof ContainerButtonPositionScreen) {
            return;
        }

        ClientUiAdapter.showScreen(minecraft, new ContainerButtonPositionScreen(current));
    }

    private static ContainerButtonConfigStore resolveStore() {
        ContainerButtonConfigStore existing = STORE.get();
        if (existing != null) {
            return existing;
        }

        Path configPath = Minecraft.getInstance()
                .gameDirectory
                .toPath()
                .resolve("config")
                .resolve(CONFIG_FILE_NAME);
        ContainerButtonConfigStore created = new ContainerButtonConfigStore(configPath);
        if (STORE.compareAndSet(null, created)) {
            return created;
        }
        return STORE.get();
    }
}
