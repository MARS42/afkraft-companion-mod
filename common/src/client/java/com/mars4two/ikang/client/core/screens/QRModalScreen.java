package com.mars4two.ikang.client.core.screens;

import com.mars4two.ikang.client.core.util.QRGenerator;
import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class QRModalScreen extends Screen {

    enum ViewState {
        INITIAL_WARNING,
        SHOW_QR,
        URL_COPIED;

        ViewState onShowQr() {
            return SHOW_QR;
        }

        ViewState onCopyUrl() {
            return URL_COPIED;
        }

        ViewState onUseQr() {
            return SHOW_QR;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("AFKraft-QRModalScreen");
    private static final int QR_SIZE = 196;
    private static final int BUTTON_HEIGHT = 20;
    private static final int INITIAL_BUTTON_WIDTH = 100;
    private static final int INITIAL_BUTTON_SPACING = 6;
    private static final int ACTION_BUTTON_WIDTH = 120;
    private static final int ACTION_BUTTON_SPACING = 12;
    private static final int LINE_HEIGHT = 12;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private final String url;
    private DynamicTexture qrTexture;
    private Identifier qrTextureId;
    private volatile Runnable onClosed = () -> {};
    private volatile Runnable onCancel = () -> {};
    private ViewState viewState = ViewState.INITIAL_WARNING;
    private final Component allowShowQrMessage1;
    private final Component allowShowQrMessage2;
    private final Component urlCopiedMessage;

    public QRModalScreen(String url) {
        super(AFKraftTranslations.translatable(AFKraftTranslations.GUI_CONNECT_QR_TITLE));
        this.url = url;
        this.allowShowQrMessage1 = AFKraftTranslations.translatable(AFKraftTranslations.GUI_CONNECT_QR_WARN_SHOW);
        this.allowShowQrMessage2 = AFKraftTranslations.translatable(AFKraftTranslations.GUI_CONNECT_QR_WARN_PRIVACY)
                .withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
        this.urlCopiedMessage = AFKraftTranslations.translatable(AFKraftTranslations.GUI_CONNECT_QR_URL_COPIED);
    }

    public void setOnClosed(Runnable onClosed) {
        this.onClosed = onClosed;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    @Override
    protected void init() {
        super.init();

        if (this.viewState == ViewState.SHOW_QR) {
            ensureQrTexture();
        }

        rebuildWidgetsForState();
    }

    private void ensureQrTexture() {
        if (this.qrTexture != null) {
            return;
        }

        Identifier obsidian = WorldAndEntityAdapter.defaultId("textures/block/obsidian.png");
        Identifier ironBlock = WorldAndEntityAdapter.defaultId("textures/block/iron_block.png");

        var nativeImage = QRGenerator.generateBlockyQR(this.url, obsidian, ironBlock);
        if (nativeImage != null) {
            this.qrTextureId = WorldAndEntityAdapter.id("afkraft", "connect_qr_code");
            this.qrTexture = ClientUiAdapter.registerDynamicTexture(
                    Minecraft.getInstance(),
                    this.qrTextureId,
                    this.url,
                    nativeImage
            );
            LOGGER.info("QR code image generated and registered successfully");
        }
    }

    private void showQrView() {
        this.viewState = this.viewState == ViewState.URL_COPIED
                ? this.viewState.onUseQr()
                : this.viewState.onShowQr();
        ensureQrTexture();
        rebuildWidgetsForState();
    }

    private void copyUrlAndShowCopiedView() {
        Minecraft client = this.minecraft != null ? this.minecraft : Minecraft.getInstance();
        ClientUiAdapter.setClipboard(client, this.url);
        this.viewState = this.viewState.onCopyUrl();
        rebuildWidgetsForState();
    }

    private void rebuildWidgetsForState() {
        this.clearWidgets();

        int centerButtonsY = this.height / 2 + 15;
        int bottomButtonsY = this.height - (BUTTON_HEIGHT + 4);

        switch (this.viewState) {
            case INITIAL_WARNING -> {
                int totalWidth = (INITIAL_BUTTON_WIDTH * 3) + (INITIAL_BUTTON_SPACING * 2);
                int startX = (this.width - totalWidth) / 2;

                Button showQrButton = Button.builder(
                                AFKraftTranslations.translatable(AFKraftTranslations.GUI_BTN_SHOW_QR),
                                button -> showQrView()
                        )
                        .bounds(startX, centerButtonsY, INITIAL_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build();

                Button copyUrlButton = Button.builder(
                                AFKraftTranslations.translatable(AFKraftTranslations.GUI_BTN_COPY_URL),
                                button -> copyUrlAndShowCopiedView()
                        )
                        .bounds(
                                startX + INITIAL_BUTTON_WIDTH + INITIAL_BUTTON_SPACING,
                                centerButtonsY,
                                INITIAL_BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build();

                Button laterButton = Button.builder(
                                AFKraftTranslations.translatable(AFKraftTranslations.GUI_BTN_LATER),
                                button -> onClose()
                        )
                        .bounds(
                                startX + (INITIAL_BUTTON_WIDTH + INITIAL_BUTTON_SPACING) * 2,
                                centerButtonsY,
                                INITIAL_BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build();

                this.addRenderableWidget(showQrButton);
                this.addRenderableWidget(copyUrlButton);
                this.addRenderableWidget(laterButton);
            }
            case SHOW_QR -> {
                int totalWidth = (ACTION_BUTTON_WIDTH * 2) + ACTION_BUTTON_SPACING;
                int startX = (this.width - totalWidth) / 2;

                Button copyUrlButton = Button.builder(
                                AFKraftTranslations.translatable(AFKraftTranslations.GUI_BTN_COPY_URL),
                                button -> copyUrlAndShowCopiedView()
                        )
                        .bounds(startX, bottomButtonsY, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build();

                Button cancelConnectButton = Button.builder(
                                AFKraftTranslations.translatable(AFKraftTranslations.GUI_BTN_CANCEL),
                                button -> onCancel()
                        )
                        .bounds(
                                startX + ACTION_BUTTON_WIDTH + ACTION_BUTTON_SPACING,
                                bottomButtonsY,
                                ACTION_BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build();

                this.addRenderableWidget(copyUrlButton);
                this.addRenderableWidget(cancelConnectButton);
            }
            case URL_COPIED -> {
                int totalWidth = (ACTION_BUTTON_WIDTH * 2) + ACTION_BUTTON_SPACING;
                int startX = (this.width - totalWidth) / 2;

                Button useQrButton = Button.builder(
                                AFKraftTranslations.translatable(AFKraftTranslations.GUI_BTN_USE_QR),
                                button -> showQrView()
                        )
                        .bounds(startX, centerButtonsY, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build();

                Button cancelConnectButton = Button.builder(
                                AFKraftTranslations.translatable(AFKraftTranslations.GUI_BTN_CANCEL),
                                button -> onCancel()
                        )
                        .bounds(
                                startX + ACTION_BUTTON_WIDTH + ACTION_BUTTON_SPACING,
                                centerButtonsY,
                                ACTION_BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build();

                this.addRenderableWidget(useQrButton);
                this.addRenderableWidget(cancelConnectButton);
            }
        }
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        renderModalContents(graphics);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);

        renderModalContents(graphics);
    }
    *///?}

    private void renderModalContents(
            //? if >=26.1 {
            GuiGraphicsExtractor graphics
            //?} else {
            /*GuiGraphics graphics
            *///?}
    ) {
        switch (this.viewState) {
            case INITIAL_WARNING -> {
                int maxTextWidth = Math.max(180, this.width - 40);
                List<FormattedCharSequence> line1 = this.font.split(allowShowQrMessage1, maxTextWidth);
                List<FormattedCharSequence> line2 = this.font.split(allowShowQrMessage2, maxTextWidth);
                int totalLines = line1.size() + line2.size();
                int startY = this.height / 2 - 20 - (totalLines * LINE_HEIGHT);
                int nextY = drawWrappedCenteredLines(graphics, line1, startY, COLOR_TEXT);
                drawWrappedCenteredLines(graphics, line2, nextY + 6, COLOR_TEXT);
            }
            case SHOW_QR -> {
                if (this.qrTextureId != null) {
                    int x = (this.width - QR_SIZE) / 2;
                    int y = (this.height - QR_SIZE) / 2;

                    ClientUiAdapter.blitQrTexture(graphics, this.qrTextureId, x, y, QR_SIZE);
                }
            }
            case URL_COPIED -> {
                int maxTextWidth = Math.max(180, this.width - 40);
                List<FormattedCharSequence> lines = this.font.split(urlCopiedMessage, maxTextWidth);
                int totalHeight = lines.size() * LINE_HEIGHT;
                int startY = this.height / 2 - 10 - totalHeight;
                drawWrappedCenteredLines(graphics, lines, startY, COLOR_TEXT);
            }
        }
    }

    private int drawWrappedCenteredLines(
            //? if >=26.1 {
            GuiGraphicsExtractor graphics,
            //?} else {
            /*GuiGraphics graphics,
            *///?}
            List<FormattedCharSequence> lines,
            int startY,
            int color
    ) {
        int y = startY;
        for (FormattedCharSequence line : lines) {
            ClientUiAdapter.drawCenteredText(graphics, this.font, line, this.width / 2, y, color);
            y += LINE_HEIGHT;
        }
        return y;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.qrTexture != null) {
            ClientUiAdapter.releaseDynamicTexture(Minecraft.getInstance(), this.qrTextureId, this.qrTexture);
            this.qrTexture = null;
            LOGGER.info("QR code image released successfully");
        }
        this.onClosed.run();
    }

    private void onCancel() {
        this.onCancel.run();
        this.onClose();
    }

    //? if >=1.21.9 {
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ClientUiAdapter.isEscapeKey(event) && this.shouldCloseOnEsc()) {
            this.onCancel();
            return true;
        }
        return super.keyPressed(event);
    }
    //?} else {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientUiAdapter.isEscapeKey(keyCode) && this.shouldCloseOnEsc()) {
            this.onCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
