package com.mars4two.ikang.client.core.screens;

import com.mars4two.ikang.client.core.preferences.ClientPreferencesService;
import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.core.snapshots.SnapshotType;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full-screen vanilla-style configuration UI for enabling or disabling AFKraft snapshots.
 */
public class SnapshotPreferencesScreen extends Screen {

    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_HEIGHT = 36;
    private static final int ROW_HEIGHT = 26;
    private static final int MAX_ROW_WIDTH = 340;
    private static final int TOGGLE_WIDTH = 72;
    private static final int TOGGLE_HEIGHT = 20;
    private static final int TOOLTIP_MAX_WIDTH = 240;

    private static final int COLOR_TEXT_EDITABLE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_READONLY = 0xFFA0A0A0;
    private static final int COLOR_BANNER_INFO = 0xFFA0A0A0;
    private static final int COLOR_BANNER_SUCCESS = 0xFF55FF55;
    private static final int COLOR_BANNER_ERROR = 0xFFFF5555;
    private static final int COLOR_BANNER_READONLY = 0xFFFFAA00;

    private final Screen parentScreen;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);

    private boolean loading;
    private boolean saving;
    private boolean editable;
    private Map<String, Boolean> committedSnapshots = new LinkedHashMap<>();
    private Map<String, Boolean> draftSnapshots = new LinkedHashMap<>();
    private Component statusMessage;
    private int statusColor = COLOR_BANNER_INFO;

    private SnapshotListWidget listWidget;
    private Button saveButton;
    private Button revertButton;
    private Button exitButton;

    private SnapshotPreferencesScreen(
            Screen parentScreen,
            boolean loading,
            boolean editable,
            Map<String, Boolean> snapshots,
            String initialErrorKey
    ) {
        super(AFKraftTranslations.translatable(AFKraftTranslations.GUI_PREFS_TITLE));
        this.parentScreen = parentScreen;
        this.loading = loading;
        this.editable = editable;
        if (!loading) {
            setCommittedState(snapshots);
            updateDefaultStatusBanner(initialErrorKey);
        } else {
            this.statusMessage = AFKraftTranslations.translatable(AFKraftTranslations.GUI_PREFS_LOADING);
            this.statusColor = COLOR_BANNER_INFO;
        }
    }

    public static SnapshotPreferencesScreen loading(Screen parentScreen) {
        return new SnapshotPreferencesScreen(parentScreen, true, false, Map.of(), "");
    }

    public static SnapshotPreferencesScreen loaded(
            Screen parentScreen,
            boolean editable,
            Map<String, Boolean> snapshots,
            String errorKey
    ) {
        return new SnapshotPreferencesScreen(parentScreen, false, editable, snapshots, errorKey);
    }

    public void applyServerSync(boolean editable, Map<String, Boolean> snapshots, String errorKey) {
        this.loading = false;
        this.saving = false;
        this.editable = editable;
        setCommittedState(snapshots);
        updateDefaultStatusBanner(errorKey);
        rebuildListEntries();
        refreshActionButtons();
    }

    public void applySaveResult(boolean success, Map<String, Boolean> serverSnapshots, String errorKey) {
        this.saving = false;
        if (success) {
            setCommittedState(serverSnapshots);
            this.statusMessage = AFKraftTranslations.translatable(AFKraftTranslations.GUI_PREFS_SAVED);
            this.statusColor = COLOR_BANNER_SUCCESS;
            rebuildListEntries();
        } else {
            if (serverSnapshots != null && !serverSnapshots.isEmpty()) {
                this.committedSnapshots = new LinkedHashMap<>(SnapshotType.mergeWithDefaults(serverSnapshots));
            }
            String key = (errorKey == null || errorKey.isBlank())
                    ? AFKraftTranslations.PREFS_ERROR_SAVE_FAILED
                    : errorKey;
            this.statusMessage = AFKraftTranslations.translatable(key);
            this.statusColor = COLOR_BANNER_ERROR;
            if (listWidget != null) {
                listWidget.syncWidgetsFromDraft();
            }
        }
        refreshActionButtons();
    }

    public void closeWithoutSaving() {
        this.draftSnapshots = new LinkedHashMap<>(this.committedSnapshots);
        if (this.minecraft != null) {
            ClientUiAdapter.showScreen(this.minecraft, this.parentScreen);
        }
    }

    @Override
    protected void init() {
        this.listWidget = this.layout.addToContents(new SnapshotListWidget(this.minecraft));

        LinearLayout footerRow = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.saveButton = footerRow.addChild(Button.builder(
                        AFKraftTranslations.translatable(AFKraftTranslations.GUI_PREFS_BTN_SAVE),
                        btn -> onSaveClicked()
                )
                .width(96)
                .build());
        this.revertButton = footerRow.addChild(Button.builder(
                        AFKraftTranslations.translatable(AFKraftTranslations.GUI_PREFS_BTN_REVERT),
                        btn -> onRevertClicked()
                )
                .width(96)
                .build());
        this.exitButton = footerRow.addChild(Button.builder(
                        AFKraftTranslations.translatable(AFKraftTranslations.GUI_PREFS_BTN_EXIT),
                        btn -> closeWithoutSaving()
                )
                .width(96)
                .build());

        this.layout.visitWidgets(this::addRenderableWidget);
        rebuildListEntries();
        repositionElements();
        refreshActionButtons();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.listWidget != null) {
            this.listWidget.updateSize(this.width, this.layout);
        }
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderHeaderAndStatus(graphics);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderHeaderAndStatus(graphics);
    }
    *///?}

    private void renderHeaderAndStatus(
            //? if >=26.1 {
            GuiGraphicsExtractor graphics
            //?} else {
            /*GuiGraphics graphics
            *///?}
    ) {
        ClientUiAdapter.drawCenteredText(graphics, this.font, this.title, this.width / 2, 10, COLOR_TEXT_EDITABLE);
        if (this.statusMessage != null) {
            ClientUiAdapter.drawCenteredText(graphics, this.font, this.statusMessage, this.width / 2, 26, this.statusColor);
        }
    }

    private void setCommittedState(Map<String, Boolean> snapshots) {
        Map<String, Boolean> merged = SnapshotType.mergeWithDefaults(snapshots);
        this.committedSnapshots = new LinkedHashMap<>(merged);
        this.draftSnapshots = new LinkedHashMap<>(merged);
    }

    private void updateDefaultStatusBanner(String errorKey) {
        if (errorKey != null && !errorKey.isBlank()) {
            this.statusMessage = AFKraftTranslations.translatable(errorKey);
            this.statusColor = COLOR_BANNER_ERROR;
        } else if (!this.editable) {
            this.statusMessage = AFKraftTranslations.translatable(AFKraftTranslations.GUI_PREFS_READONLY);
            this.statusColor = COLOR_BANNER_READONLY;
        } else {
            this.statusMessage = null;
        }
    }

    private boolean hasUnsavedChanges() {
        return !this.loading && !this.draftSnapshots.equals(this.committedSnapshots);
    }

    private void onToggleChanged(SnapshotType type, boolean newValue) {
        if (!this.editable || this.saving) {
            return;
        }
        this.draftSnapshots.put(type.snapshotName(), newValue);
        if (this.statusColor == COLOR_BANNER_SUCCESS || this.statusColor == COLOR_BANNER_ERROR) {
            this.statusMessage = null;
        }
        refreshActionButtons();
    }

    private void onSaveClicked() {
        if (!this.editable || this.saving || !hasUnsavedChanges()) {
            return;
        }
        this.saving = true;
        this.statusMessage = AFKraftTranslations.translatable(AFKraftTranslations.GUI_PREFS_SAVING);
        this.statusColor = COLOR_BANNER_INFO;
        refreshActionButtons();
        if (this.listWidget != null) {
            this.listWidget.syncWidgetsFromDraft();
        }
        ClientPreferencesService.sendSaveRequest(this.draftSnapshots);
    }

    private void onRevertClicked() {
        if (!this.editable || this.saving || !hasUnsavedChanges()) {
            return;
        }
        this.draftSnapshots = new LinkedHashMap<>(this.committedSnapshots);
        if (this.statusColor == COLOR_BANNER_SUCCESS || this.statusColor == COLOR_BANNER_ERROR) {
            this.statusMessage = null;
        }
        if (this.listWidget != null) {
            this.listWidget.syncWidgetsFromDraft();
        }
        refreshActionButtons();
    }

    private void rebuildListEntries() {
        if (this.listWidget == null) {
            return;
        }
        this.listWidget.rebuild();
    }

    private void refreshActionButtons() {
        boolean canMutate = !this.loading && !this.saving && this.editable && hasUnsavedChanges();
        if (this.saveButton != null) {
            this.saveButton.active = canMutate;
        }
        if (this.revertButton != null) {
            this.revertButton.active = canMutate;
        }
        if (this.exitButton != null) {
            this.exitButton.active = true;
        }
    }

    private final class SnapshotListWidget extends ContainerObjectSelectionList<SnapshotRowEntry> {

        private SnapshotListWidget(Minecraft minecraft) {
            super(
                    minecraft,
                    SnapshotPreferencesScreen.this.width,
                    SnapshotPreferencesScreen.this.layout.getContentHeight(),
                    SnapshotPreferencesScreen.this.layout.getHeaderHeight(),
                    ROW_HEIGHT
            );
        }

        @Override
        public int getRowWidth() {
            return Math.min(MAX_ROW_WIDTH, Math.max(200, SnapshotPreferencesScreen.this.width - 40));
        }

        private void rebuild() {
            this.clearEntries();
            if (SnapshotPreferencesScreen.this.loading) {
                return;
            }
            for (SnapshotType type : SnapshotType.values()) {
                this.addEntry(new SnapshotRowEntry(type));
            }
        }

        private void syncWidgetsFromDraft() {
            for (SnapshotRowEntry entry : this.children()) {
                entry.syncFromDraft();
            }
        }
    }

    private final class SnapshotRowEntry extends ContainerObjectSelectionList.Entry<SnapshotRowEntry> {

        private final SnapshotType snapshotType;
        private final Component label;
        private final Component description;
        private final CycleButton<Boolean> toggleButton;

        private SnapshotRowEntry(SnapshotType snapshotType) {
            this.snapshotType = snapshotType;
            this.label = AFKraftTranslations.translatable(
                    AFKraftTranslations.snapshotNameKey(snapshotType.translationSlug())
            );
            this.description = AFKraftTranslations.translatable(
                    AFKraftTranslations.snapshotDescriptionKey(snapshotType.translationSlug())
            );

            boolean initialValue = !Boolean.FALSE.equals(
                    SnapshotPreferencesScreen.this.draftSnapshots.get(snapshotType.snapshotName())
            );
            this.toggleButton = CycleButton.onOffBuilder(initialValue)
                    .displayOnlyValue()
                    .create(
                            0,
                            0,
                            TOGGLE_WIDTH,
                            TOGGLE_HEIGHT,
                            this.label,
                            (button, value) -> SnapshotPreferencesScreen.this.onToggleChanged(snapshotType, value)
                    );
            this.toggleButton.active = SnapshotPreferencesScreen.this.editable && !SnapshotPreferencesScreen.this.saving;
        }

        private void syncFromDraft() {
            boolean value = !Boolean.FALSE.equals(
                    SnapshotPreferencesScreen.this.draftSnapshots.get(this.snapshotType.snapshotName())
            );
            this.toggleButton.setValue(value);
            this.toggleButton.active = SnapshotPreferencesScreen.this.editable && !SnapshotPreferencesScreen.this.saving;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.toggleButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.toggleButton);
        }

        //? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            renderRow(graphics, getContentX(), getContentY(), getContentWidth(), getContentHeight(), mouseX, mouseY, hovering, partialTick);
        }
        //?} elif >=1.21.9 {
        /*@Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            renderRow(graphics, getContentX(), getContentY(), getContentWidth(), getContentHeight(), mouseX, mouseY, hovering, partialTick);
        }
        *///?} else {
        /*@Override
        public void render(
                GuiGraphics graphics,
                int index,
                int top,
                int left,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovering,
                float partialTick
        ) {
            renderRow(graphics, left, top, width, height, mouseX, mouseY, hovering, partialTick);
        }
        *///?}

        private void renderRow(
                //? if >=26.1 {
                GuiGraphicsExtractor graphics,
                //?} else {
                /*GuiGraphics graphics,
                *///?}
                int left,
                int top,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovering,
                float partialTick
        ) {
            int labelY = top + (height - SnapshotPreferencesScreen.this.font.lineHeight) / 2;
            int textColor = SnapshotPreferencesScreen.this.editable ? COLOR_TEXT_EDITABLE : COLOR_TEXT_READONLY;
            ClientUiAdapter.drawText(graphics, SnapshotPreferencesScreen.this.font, this.label, left + 4, labelY, textColor);

            int toggleX = left + width - TOGGLE_WIDTH - 4;
            int toggleY = top + (height - TOGGLE_HEIGHT) / 2;
            this.toggleButton.setX(toggleX);
            this.toggleButton.setY(toggleY);
            this.toggleButton.active = SnapshotPreferencesScreen.this.editable && !SnapshotPreferencesScreen.this.saving;
            ClientUiAdapter.renderWidget(graphics, this.toggleButton, mouseX, mouseY, partialTick);

            if (hovering || this.toggleButton.isMouseOver(mouseX, mouseY)) {
                List<FormattedCharSequence> tooltipLines = new ArrayList<>(
                        SnapshotPreferencesScreen.this.font.split(this.description, TOOLTIP_MAX_WIDTH)
                );
                if (!SnapshotPreferencesScreen.this.editable) {
                    Component readOnlyHint = AFKraftTranslations.translatable(
                            AFKraftTranslations.GUI_PREFS_READONLY_TOOLTIP
                    ).withStyle(ChatFormatting.GOLD);
                    tooltipLines.addAll(SnapshotPreferencesScreen.this.font.split(readOnlyHint, TOOLTIP_MAX_WIDTH));
                }
                ClientUiAdapter.renderTooltip(
                        graphics,
                        SnapshotPreferencesScreen.this.font,
                        tooltipLines,
                        mouseX,
                        mouseY
                );
            }
        }
    }
}
