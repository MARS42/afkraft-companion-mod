package com.mars4two.ikang.client.core.update;

import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.client.platform.services.ClientServices;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import com.mars4two.ikang.core.version.UpdateCheckResult;
import com.mars4two.ikang.core.version.UpdateCheckerService;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import com.mars4two.ikang.platform.services.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientUpdateNotificationManager {

    private static final Logger log = LoggerFactory.getLogger(ClientUpdateNotificationManager.class);
    private static final AtomicBoolean hasNotified = new AtomicBoolean(false);
    private static CompletableFuture<UpdateCheckResult> pendingCheck;

    private ClientUpdateNotificationManager() {
    }

    public static void init() {
        String modVersion = Services.PLATFORM_INFO.getModVersion();
        String mcVersion = Services.PLATFORM_INFO.getMinecraftVersion();
        log.info("Starting background update check for AFKraft Companion (mod: {}, mc: {})", modVersion, mcVersion);

        UpdateCheckerService checker = new UpdateCheckerService();
        pendingCheck = checker.checkAsync(modVersion, mcVersion);

        ClientServices.LIFECYCLE.onClientPlayerJoin(ClientUpdateNotificationManager::onPlayerJoin);
    }

    private static void onPlayerJoin(Minecraft client) {
        if (hasNotified.get() || pendingCheck == null) {
            return;
        }

        pendingCheck.thenAccept(result -> {
            if (result.isOutdated() && hasNotified.compareAndSet(false, true)) {
                client.execute(() -> notifyPlayer(client, result));
            }
        });
    }

    private static void notifyPlayer(Minecraft client, UpdateCheckResult result) {
        if (client.player == null) {
            return;
        }

        String latestStr = result.latestVersion().toCleanString();
        String currentStr = result.currentVersion().toCleanString();
        String downloadUrl = result.downloadUrl();

        Component downloadButton = WorldAndEntityAdapter.buildClickableUrlWord(
                AFKraftTranslations.translatable(AFKraftTranslations.CHAT_UPDATE_BUTTON),
                ChatFormatting.GREEN,
                downloadUrl,
                AFKraftTranslations.translatable(AFKraftTranslations.CHAT_UPDATE_HOVER)
        );

        Component message = AFKraftTranslations.translatable(
                AFKraftTranslations.CHAT_UPDATE_AVAILABLE,
                latestStr,
                currentStr,
                downloadButton
        ).withStyle(ChatFormatting.GOLD);

        ClientUiAdapter.sendPlayerMessage(client.player, message);
    }
}
