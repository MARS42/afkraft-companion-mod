package com.mars4two.ikang.core.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class AFKraftTranslations {

    private AFKraftTranslations() {
    }

    // Commands - Connect
    public static final String CMD_CONNECT_ALREADY = "command.afkraft.connect.already_connected";
    public static final String CMD_CONNECT_CONNECTING = "command.afkraft.connect.connecting";
    public static final String CMD_CONNECT_EXPIRED = "command.afkraft.connect.code_expired";
    public static final String CMD_CONNECT_SUCCESS = "command.afkraft.connect.success";
    public static final String CMD_CONNECT_CANCELLED = "command.afkraft.connect.cancelled";
    public static final String CMD_CONNECT_CANCEL_ERROR = "command.afkraft.connect.cancel_error";
    public static final String CMD_CONNECT_WAITING = "command.afkraft.connect.waiting_approval";
    public static final String CMD_CONNECT_AUTH_ERROR = "command.afkraft.connect.auth_error";
    public static final String CMD_CONNECT_GENERIC_ERROR = "command.afkraft.connect.generic_error";

    // Commands - Enable
    public static final String CMD_ENABLE_ALREADY_ACTIVE = "command.afkraft.enable.already_active";
    public static final String CMD_ENABLE_GENERATING = "command.afkraft.enable.generating";
    public static final String CMD_ENABLE_ERROR = "command.afkraft.enable.error";
    public static final String CMD_ENABLE_NOT_LINKED = "command.afkraft.enable.not_linked";

    // Chat - Authorize Server (Client)
    public static final String CHAT_AUTH_SERVER_SUCCESS = "chat.afkraft.authorize_server.success";
    public static final String CHAT_AUTH_SERVER_ERROR = "chat.afkraft.authorize_server.error";
    public static final String CHAT_AUTH_SERVER_RETRY = "chat.afkraft.authorize_server.failed_retry";
    public static final String CHAT_AUTH_SERVER_NOT_CONNECTED = "chat.afkraft.authorize_server.not_connected";

    // Chat - Server Admin & Player Prompts (Server)
    public static final String CHAT_ADMIN_CREDS_SUCCESS = "chat.afkraft.admin.credentials_success";
    public static final String CHAT_ADMIN_CREDS_ERROR = "chat.afkraft.admin.credentials_error";
    public static final String CHAT_SERVER_LINK_PROMPT = "chat.afkraft.server_link_prompt.message";
    public static final String CHAT_SERVER_LINK_TEXT = "chat.afkraft.server_link_prompt.link_text";
    public static final String CHAT_SERVER_LINK_HOVER = "chat.afkraft.server_link_prompt.link_hover";

    // Chat - Update Notification
    public static final String CHAT_UPDATE_AVAILABLE = "chat.afkraft.update.available";
    public static final String CHAT_UPDATE_BUTTON = "chat.afkraft.update.button_download";
    public static final String CHAT_UPDATE_HOVER = "chat.afkraft.update.hover_tooltip";

    // GUI - Authorize Server Screen
    public static final String GUI_AUTH_SERVER_TITLE = "gui.afkraft.authorize_server.title";
    public static final String GUI_AUTH_SERVER_MSG = "gui.afkraft.authorize_server.message";

    // GUI - QR Screen
    public static final String GUI_CONNECT_QR_TITLE = "gui.afkraft.connect_qr.title";
    public static final String GUI_CONNECT_QR_WARN_SHOW = "gui.afkraft.connect_qr.warning_show";
    public static final String GUI_CONNECT_QR_WARN_PRIVACY = "gui.afkraft.connect_qr.warning_privacy";

    // GUI - Buttons
    public static final String GUI_BTN_AUTHORIZE = "gui.afkraft.button.authorize";
    public static final String GUI_BTN_LATER = "gui.afkraft.button.later";
    public static final String GUI_BTN_CANCEL = "gui.afkraft.button.cancel";
    public static final String GUI_BTN_SHOW_QR = "gui.afkraft.button.show_qr";

    // GUI - Container Tracking Mode Cycle Button
    public static final String GUI_CONTAINER_PREFIX = "gui.afkraft.container_mode.prefix";
    public static final String GUI_CONTAINER_SHULKER = "gui.afkraft.container_mode.shulker";
    public static final String GUI_CONTAINER_STATION = "gui.afkraft.container_mode.load_station";
    public static final String GUI_CONTAINER_OFF = "gui.afkraft.container_mode.off";

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
