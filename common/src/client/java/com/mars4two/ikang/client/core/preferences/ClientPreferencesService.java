package com.mars4two.ikang.client.core.preferences;

import com.mars4two.ikang.client.core.screens.SnapshotPreferencesScreen;
import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.core.network.payloads.client.SaveSnapshotPreferencesResultClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.SnapshotPreferencesSyncClientboundPayload;
import com.mars4two.ikang.core.network.payloads.server.RequestSnapshotPreferencesServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.UpdateSnapshotPreferencesServerboundPayload;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import com.mars4two.ikang.platform.services.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * Coordinates client-side snapshot preferences UI opening, synchronization, and save responses.
 */
public final class ClientPreferencesService {

    private ClientPreferencesService() {
    }

    public static void requestOpenPreferencesScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }

        Screen current = ClientUiAdapter.currentScreen(minecraft);
        if (current instanceof SnapshotPreferencesScreen) {
            return;
        }

        SnapshotPreferencesScreen loadingScreen = SnapshotPreferencesScreen.loading(current);
        ClientUiAdapter.showScreen(minecraft, loadingScreen);
        Services.NETWORK.sendToServer(new RequestSnapshotPreferencesServerboundPayload());
    }

    public static void sendSaveRequest(Map<String, Boolean> draftSnapshots) {
        Services.NETWORK.sendToServer(new UpdateSnapshotPreferencesServerboundPayload(draftSnapshots));
    }

    public static void onPreferencesSync(SnapshotPreferencesSyncClientboundPayload payload, Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Screen current = ClientUiAdapter.currentScreen(minecraft);

            if (!payload.authenticated()) {
                if (current instanceof SnapshotPreferencesScreen prefsScreen) {
                    prefsScreen.closeWithoutSaving();
                }
                if (minecraft.player != null) {
                    String key = payload.errorKey().isBlank()
                            ? AFKraftTranslations.PREFS_ERROR_NOT_AUTHENTICATED
                            : payload.errorKey();
                    ClientUiAdapter.sendPlayerMessage(
                            minecraft.player,
                            AFKraftTranslations.translatable(key).withStyle(ChatFormatting.YELLOW)
                    );
                }
                return;
            }

            if (current instanceof SnapshotPreferencesScreen prefsScreen) {
                prefsScreen.applyServerSync(payload.editable(), payload.snapshots(), payload.errorKey());
            } else {
                SnapshotPreferencesScreen screen = SnapshotPreferencesScreen.loaded(
                        current,
                        payload.editable(),
                        payload.snapshots(),
                        payload.errorKey()
                );
                ClientUiAdapter.showScreen(minecraft, screen);
            }
        });
    }

    public static void onSaveResult(SaveSnapshotPreferencesResultClientboundPayload payload, Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Screen current = ClientUiAdapter.currentScreen(minecraft);
            if (current instanceof SnapshotPreferencesScreen prefsScreen) {
                prefsScreen.applySaveResult(payload.success(), payload.snapshots(), payload.errorKey());
            }
        });
    }
}
