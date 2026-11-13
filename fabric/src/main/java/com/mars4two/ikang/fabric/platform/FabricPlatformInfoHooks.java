package com.mars4two.ikang.fabric.platform;

import com.mars4two.ikang.platform.services.PlatformInfoHooks;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricPlatformInfoHooks implements PlatformInfoHooks {

    @Override
    public String getModVersion() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            if (loader == null) return "1.0.0";
            return loader.getModContainer("afkraft_companion")
                    .map(m -> m.getMetadata().getVersion().getFriendlyString())
                    .orElse("1.0.0");
        } catch (Throwable t) {
            return "1.0.0";
        }
    }

    @Override
    public String getMinecraftVersion() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            if (loader == null) return "unknown";
            return loader.getModContainer("minecraft")
                    .map(m -> m.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        } catch (Throwable t) {
            return "unknown";
        }
    }

    @Override
    public String getLoaderName() {
        return "Fabric";
    }
}
