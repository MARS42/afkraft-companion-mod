package com.mars4two.ikang.neoforge.platform;

import com.mars4two.ikang.platform.services.PlatformInfoHooks;
import net.neoforged.fml.ModList;

public final class NeoForgePlatformInfoHooks implements PlatformInfoHooks {

    @Override
    public String getModVersion() {
        ModList modList = ModList.get();
        if (modList == null) {
            return "1.0.0";
        }
        return modList.getModContainerById("afkraft_companion")
                .map(m -> m.getModInfo().getVersion().toString())
                .orElse("1.0.0");
    }

    @Override
    public String getMinecraftVersion() {
        ModList modList = ModList.get();
        if (modList == null) {
            return "unknown";
        }
        return modList.getModContainerById("minecraft")
                .map(m -> m.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public String getLoaderName() {
        return "NeoForge";
    }
}
