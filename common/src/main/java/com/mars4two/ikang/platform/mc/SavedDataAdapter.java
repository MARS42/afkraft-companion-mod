package com.mars4two.ikang.platform.mc;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import net.minecraft.server.MinecraftServer;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedDataType;
//?} elif >=1.21.5 {
/*import net.minecraft.world.level.saveddata.SavedDataType;
*///?} else {
/*import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
*///?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Isolates version-specific {@code SavedData} registration, lookup, and legacy migration
 * between Minecraft 1.21.x ({@code SavedData.Factory}) and 1.21.5+/26.x ({@code SavedDataType}).
 */
public final class SavedDataAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SavedDataAdapter.class);

    //? if >=26.1 {
    private static final Identifier CURRENT_ID =
            WorldAndEntityAdapter.id(AFKraftCompanion.MOD_ID, "afk_container_config_data");
    private static final Identifier LEGACY_ID =
            WorldAndEntityAdapter.id(AFKraftCompanion.LEGACY_MOD_ID, "afk_container_config_data");

    public static final SavedDataType<AFKContainerConfigData> CURRENT_TYPE =
            new SavedDataType<>(CURRENT_ID, AFKContainerConfigData::new, AFKContainerConfigData.CODEC, null);
    private static final SavedDataType<AFKContainerConfigData> LEGACY_TYPE =
            new SavedDataType<>(LEGACY_ID, AFKContainerConfigData::new, AFKContainerConfigData.CODEC, null);
    //?} elif >=1.21.5 {
    /*private static final String CURRENT_KEY = AFKraftCompanion.MOD_ID + "_afk_container_config_data";
    private static final String LEGACY_KEY = AFKraftCompanion.LEGACY_MOD_ID + "_afk_container_config_data";

    public static final SavedDataType<AFKContainerConfigData> CURRENT_TYPE =
            new SavedDataType<>(CURRENT_KEY, AFKContainerConfigData::new, AFKContainerConfigData.CODEC, null);
    private static final SavedDataType<AFKContainerConfigData> LEGACY_TYPE =
            new SavedDataType<>(LEGACY_KEY, AFKContainerConfigData::new, AFKContainerConfigData.CODEC, null);
    *///?} else {
    /*private static final String CURRENT_KEY = AFKraftCompanion.MOD_ID + "_afk_container_config_data";
    private static final String LEGACY_KEY = AFKraftCompanion.LEGACY_MOD_ID + "_afk_container_config_data";

    public static final SavedData.Factory<AFKContainerConfigData> FACTORY =
            new SavedData.Factory<>(AFKContainerConfigData::new, AFKContainerConfigData::load, DataFixTypes.LEVEL);
    *///?}

    private SavedDataAdapter() {
    }

    public static AFKContainerConfigData getOrCreateConfig(MinecraftServer server) {
        if (server == null) {
            return new AFKContainerConfigData();
        }
        var storage = server.overworld().getDataStorage();
        //? if >=1.21.5 {
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
            //? if >=26.1 {
            LOG.info("Migrated legacy AFKContainerConfigData from {} to {}", LEGACY_ID, CURRENT_ID);
            //?} else {
            /*LOG.info("Migrated legacy AFKContainerConfigData from {} to {}", LEGACY_KEY, CURRENT_KEY);
            *///?}
            return migrated;
        }
        return storage.computeIfAbsent(CURRENT_TYPE);
        //?} else {
        /*var current = storage.get(FACTORY, CURRENT_KEY);
        if (current != null) {
            return current;
        }
        var legacy = storage.get(FACTORY, LEGACY_KEY);
        if (legacy != null) {
            var migrated = new AFKContainerConfigData(
                    legacy.getConfig(),
                    legacy.exportPortableContainers()
            );
            migrated.setDirty();
            storage.set(CURRENT_KEY, migrated);
            LOG.info("Migrated legacy AFKContainerConfigData from {} to {}", LEGACY_KEY, CURRENT_KEY);
            return migrated;
        }
        return storage.computeIfAbsent(FACTORY, CURRENT_KEY);
        *///?}
    }
}
