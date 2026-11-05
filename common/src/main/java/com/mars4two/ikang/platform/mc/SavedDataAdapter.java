package com.mars4two.ikang.platform.mc;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Isolates version-specific {@code SavedData} registration, lookup, and legacy migration
 * between Minecraft 1.21.x ({@code SavedData.Factory}) and 26.2+ ({@code SavedDataType}).
 */
public final class SavedDataAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SavedDataAdapter.class);

    private static final Identifier CURRENT_ID =
            WorldAndEntityAdapter.id(AFKraftCompanion.MOD_ID, "afk_container_config_data");
    private static final Identifier LEGACY_ID =
            WorldAndEntityAdapter.id(AFKraftCompanion.LEGACY_MOD_ID, "afk_container_config_data");

    public static final SavedDataType<AFKContainerConfigData> CURRENT_TYPE =
            new SavedDataType<>(CURRENT_ID, AFKContainerConfigData::new, AFKContainerConfigData.CODEC, null);
    private static final SavedDataType<AFKContainerConfigData> LEGACY_TYPE =
            new SavedDataType<>(LEGACY_ID, AFKContainerConfigData::new, AFKContainerConfigData.CODEC, null);

    private SavedDataAdapter() {
    }

    public static AFKContainerConfigData getOrCreateConfig(MinecraftServer server) {
        if (server == null) {
            return new AFKContainerConfigData();
        }
        var storage = server.overworld().getDataStorage();
        var current = storage.get(CURRENT_TYPE);
        if (current != null) {
            return current;
        }
        var legacy = storage.get(LEGACY_TYPE);
        if (legacy != null) {
            var migrated = new AFKContainerConfigData(
                    legacy.getConfig(),
                    legacy.exportPortableContainers()
            );
            migrated.setDirty();
            storage.set(CURRENT_TYPE, migrated);
            LOG.info("Migrated legacy AFKContainerConfigData from {} to {}", LEGACY_ID, CURRENT_ID);
            return migrated;
        }
        return storage.computeIfAbsent(CURRENT_TYPE);
    }
}
