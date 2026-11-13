package com.mars4two.ikang.platform.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public final class Services {

    private static final Logger LOG = LoggerFactory.getLogger(Services.class);

    public static final NetworkBridge NETWORK = load(NetworkBridge.class);
    public static final ServerLifecycleHooks SERVER_LIFECYCLE = load(ServerLifecycleHooks.class);
    public static final BlockInteractionHooks BLOCK_INTERACTION = load(BlockInteractionHooks.class);
    public static final PlatformInfoHooks PLATFORM_INFO = load(PlatformInfoHooks.class);

    private Services() {
    }

    public static <T> T load(Class<T> serviceClass) {
        T loaded = ServiceLoader.load(serviceClass, serviceClass.getClassLoader())
                .findFirst()
                .orElseGet(() -> ServiceLoader.load(serviceClass)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Failed to load platform service implementation for " + serviceClass.getName()
                        )));
        LOG.debug("Loaded {} for service {}", loaded.getClass().getName(), serviceClass.getName());
        return loaded;
    }
}
