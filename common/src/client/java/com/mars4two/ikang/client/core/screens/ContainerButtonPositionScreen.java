package com.mars4two.ikang.client.core.screens;

import com.mars4two.ikang.client.core.preferences.ClientContainerButtonConfigService;
import com.mars4two.ikang.client.core.preferences.ContainerButtonPosition;
import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import com.mojang.blaze3d.platform.InputConstants;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Interactive vanilla-style screen for positioning the AFK container toggle button.
 */
public class ContainerButtonPositionScreen extends Screen {

    private static final int HEADER_HEIGHT = 48;
    private static final int FOOTER_HEIGHT = 36;
    private static final int FOOTER_BUTTON_WIDTH = 80;
    private static final int PREVIEW_TOGGLE_WIDTH = 132;
    private static final int PREVIEW_TOGGLE_HEIGHT = 18;

    private static final int CONTAINER_PREVIEW_WIDTH = 176;
    private static final int CONTAINER_PREVIEW_HEIGHT = 166;
    private static final int STANDARD_BUTTON_WIDTH = 48;
    private static final int SHULKER_BUTTON_WIDTH = 84;
    private static final int PREVIEW_BUTTON_HEIGHT = 16;

    private static final int NUDGE_STEP_PX = 1;
    private static final int FAST_NUDGE_STEP_PX = 10;

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_BANNER_INFO = 0xFFA0A0A0;
    private static final int COLOR_BANNER_SUCCESS = 0xFF55FF55;
    private static final int COLOR_BANNER_ERROR = 0xFFFF5555;
    private static final int COLOR_COORDS = 0xFFE0E0E0;

    private static final int COLOR_PANEL_BG = 0xD8C6C6C6;
    private static final int COLOR_PANEL_BORDER = 0xFF000000;
    private static final int COLOR_PANEL_HIGHLIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_SHADOW = 0xFF555555;
    private static final int COLOR_PANEL_TEXT = 0xFF404040;
    private static final int COLOR_SLOT_BG = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_SLOT_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_ANCHOR_MARKER = 0xFF55FFFF;
    private static final int COLOR_GUIDE_LINE = 0x8055FFFF;
    private static final int COLOR_HOVER_OUTLINE = 0xFF55FF55;
    private static final int COLOR_DRAG_OUTLINE = 0xFFFFFF55;

    private final Screen parentScreen;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);

    private ContainerButtonPosition committedPosition;
    private ContainerButtonPosition draftPosition;
    private boolean previewShulkerMode;
    private boolean dragging;
    private int dragGrabOffsetX;
    private int dragGrabOffsetY;

    private Component statusMessage;
    private int statusColor = COLOR_BANNER_INFO;

    private AbstractWidget standardPreviewButton;
    private AbstractWidget shulkerPreviewButton;
    private Button previewModeToggleButton;
    private Button saveButton;
    private Button revertButton;
    private Button defaultButton;
    private Button cancelButton;

    public ContainerButtonPositionScreen(Screen parentScreen) {
        super(AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_TITLE));
        this.parentScreen = parentScreen;
        this.committedPosition = ClientContainerButtonConfigService.getPosition();
        this.draftPosition = this.committedPosition;
        this.statusMessage = AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_HINT);
    }

    @Override
    protected void init() {
        this.standardPreviewButton = CycleButton.onOffBuilder(true)
                .create(
                        0,
                        0,
                        STANDARD_BUTTON_WIDTH,
                        PREVIEW_BUTTON_HEIGHT,
                        AFKraftTranslations.translatable(AFKraftTranslations.GUI_CONTAINER_PREFIX),
                        (button, value) -> {}
                );
        this.shulkerPreviewButton = Button.builder(
                        AFKraftTranslations.translatable(
                                AFKraftTranslations.GUI_BUTTON_POS_PREVIEW_LABEL,
                                AFKraftTranslations.translatable(AFKraftTranslations.GUI_CONTAINER_PREFIX),
                                AFKraftTranslations.translatable(AFKraftTranslations.GUI_CONTAINER_SHULKER)
                        ),
                        button -> {}
                )
                .bounds(0, 0, SHULKER_BUTTON_WIDTH, PREVIEW_BUTTON_HEIGHT)
                .build();

        this.previewModeToggleButton = Button.builder(
                        previewModeToggleLabel(),
                        button -> {
                            this.previewShulkerMode = !this.previewShulkerMode;
                            button.setMessage(previewModeToggleLabel());
                        }
                )
                .bounds(8, 8, PREVIEW_TOGGLE_WIDTH, PREVIEW_TOGGLE_HEIGHT)
                .build();
        this.addRenderableWidget(this.previewModeToggleButton);

        LinearLayout footerRow = this.layout.addToFooter(LinearLayout.horizontal().spacing(6));
        this.saveButton = footerRow.addChild(Button.builder(
                        AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_BTN_SAVE),
                        button -> onSaveClicked()
                )
                .width(FOOTER_BUTTON_WIDTH)
                .build());
        this.revertButton = footerRow.addChild(Button.builder(
                        AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_BTN_REVERT),
                        button -> onRevertClicked()
                )
                .width(FOOTER_BUTTON_WIDTH)
                .build());
        this.defaultButton = footerRow.addChild(Button.builder(
                        AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_BTN_DEFAULT),
                        button -> onDefaultClicked()
                )
                .width(FOOTER_BUTTON_WIDTH)
                .build());
        this.cancelButton = footerRow.addChild(Button.builder(
                        AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_BTN_CANCEL),
                        button -> closeAndRevert()
                )
                .width(FOOTER_BUTTON_WIDTH)
                .build());

        this.layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
        refreshActionButtons();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.previewModeToggleButton != null) {
            this.previewModeToggleButton.setX(8);
            this.previewModeToggleButton.setY(8);
        }
    }

    @Override
    public void onClose() {
        closeAndRevert();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderPreviewAndHeader(graphics, mouseX, mouseY, partialTick);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPreviewAndHeader(graphics, mouseX, mouseY, partialTick);
    }
    *///?}

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        return handlePreviewMouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (handlePreviewMouseDragged(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (handlePreviewMouseReleased(event.button())) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (handleNudgeKey(
                ClientUiAdapter.isLeftKey(event),
                ClientUiAdapter.isRightKey(event),
                ClientUiAdapter.isUpKey(event),
                ClientUiAdapter.isDownKey(event),
                ClientUiAdapter.isShiftDown(event)
        )) {
            return true;
        }
        return super.keyPressed(event);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return handlePreviewMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (handlePreviewMouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (handlePreviewMouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (handleNudgeKey(
                ClientUiAdapter.isLeftKey(keyCode),
                ClientUiAdapter.isRightKey(keyCode),
                ClientUiAdapter.isUpKey(keyCode),
                ClientUiAdapter.isDownKey(keyCode),
                ClientUiAdapter.isShiftDown(modifiers)
        )) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?}

    private void renderPreviewAndHeader(
            //? if >=26.1 {
            GuiGraphicsExtractor graphics,
            //?} else {
            /*GuiGraphics graphics,
            *///?}
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderHeader(graphics);

        int containerLeft = containerPreviewLeft();
        int containerTop = containerPreviewTop();
        renderContainerPreview(graphics, containerLeft, containerTop);

        int previewWidth = activePreviewButtonWidth();
        ContainerButtonPosition.ResolvedPosition resolved = resolveDraftPosition(containerLeft, containerTop, previewWidth);
        int anchorX = containerLeft + CONTAINER_PREVIEW_WIDTH;
        int anchorY = containerTop;

        if (this.dragging) {
            renderAnchorGuides(graphics, anchorX, anchorY, resolved.x(), resolved.y());
        }

        // Small anchor marker at the container top-right corner
        ClientUiAdapter.fillRect(graphics, anchorX - 2, anchorY - 2, anchorX + 3, anchorY + 3, COLOR_ANCHOR_MARKER);

        AbstractWidget previewWidget = activePreviewWidget();
        if (previewWidget != null) {
            previewWidget.setX(resolved.x());
            previewWidget.setY(resolved.y());
            ClientUiAdapter.renderWidget(graphics, previewWidget, mouseX, mouseY, partialTick);

            boolean hovered = isInsideRect(mouseX, mouseY, resolved.x(), resolved.y(), previewWidth, PREVIEW_BUTTON_HEIGHT);
            if (this.dragging || hovered) {
                int outlineColor = this.dragging ? COLOR_DRAG_OUTLINE : COLOR_HOVER_OUTLINE;
                ClientUiAdapter.drawOutline(
                        graphics,
                        resolved.x() - 1,
                        resolved.y() - 1,
                        previewWidth + 2,
                        PREVIEW_BUTTON_HEIGHT + 2,
                        outlineColor
                );
            }
        }
    }

    private void renderHeader(
            //? if >=26.1 {
            GuiGraphicsExtractor graphics
            //?} else {
            /*GuiGraphics graphics
            *///?}
    ) {
        ClientUiAdapter.drawCenteredText(graphics, this.font, this.title, this.width / 2, 8, COLOR_TITLE);
        if (this.statusMessage != null) {
            ClientUiAdapter.drawCenteredText(graphics, this.font, this.statusMessage, this.width / 2, 21, this.statusColor);
        }
        Component coordsText = AFKraftTranslations.translatable(
                AFKraftTranslations.GUI_BUTTON_POS_COORDS,
                this.draftPosition.offsetX(),
                this.draftPosition.offsetY()
        );
        ClientUiAdapter.drawCenteredText(graphics, this.font, coordsText, this.width / 2, 34, COLOR_COORDS);
    }

    private void renderContainerPreview(
            //? if >=26.1 {
            GuiGraphicsExtractor graphics,
            //?} else {
            /*GuiGraphics graphics,
            *///?}
            int left,
            int top
    ) {
        int right = left + CONTAINER_PREVIEW_WIDTH;
        int bottom = top + CONTAINER_PREVIEW_HEIGHT;

        // Vanilla-style beveled container panel
        ClientUiAdapter.fillRect(graphics, left, top, right, bottom, COLOR_PANEL_BG);
        ClientUiAdapter.drawOutline(graphics, left, top, CONTAINER_PREVIEW_WIDTH, CONTAINER_PREVIEW_HEIGHT, COLOR_PANEL_BORDER);
        ClientUiAdapter.fillRect(graphics, left + 1, top + 1, right - 1, top + 3, COLOR_PANEL_HIGHLIGHT);
        ClientUiAdapter.fillRect(graphics, left + 1, top + 1, left + 3, bottom - 1, COLOR_PANEL_HIGHLIGHT);
        ClientUiAdapter.fillRect(graphics, left + 2, bottom - 3, right - 1, bottom - 1, COLOR_PANEL_SHADOW);
        ClientUiAdapter.fillRect(graphics, right - 3, top + 2, right - 1, bottom - 1, COLOR_PANEL_SHADOW);

        Component containerLabel = AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_CONTAINER_LABEL);
        ClientUiAdapter.drawText(graphics, this.font, containerLabel, left + 8, top + 6, COLOR_PANEL_TEXT);

        // Top 9x3 chest slots
        renderSlotGrid(graphics, left + 7, top + 17, 9, 3);
        // Player 9x3 inventory slots
        renderSlotGrid(graphics, left + 7, top + 83, 9, 3);
        // Player 9x1 hotbar slots
        renderSlotGrid(graphics, left + 7, top + 141, 9, 1);
    }

    private void renderSlotGrid(
            //? if >=26.1 {
            GuiGraphicsExtractor graphics,
            //?} else {
            /*GuiGraphics graphics,
            *///?}
            int startX,
            int startY,
            int cols,
            int rows
    ) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = startX + col * 18;
                int y = startY + row * 18;
                ClientUiAdapter.fillRect(graphics, x, y, x + 18, y + 18, COLOR_SLOT_DARK);
                ClientUiAdapter.fillRect(graphics, x + 1, y + 1, x + 18, y + 18, COLOR_SLOT_LIGHT);
                ClientUiAdapter.fillRect(graphics, x + 1, y + 1, x + 17, y + 17, COLOR_SLOT_BG);
            }
        }
    }

    private void renderAnchorGuides(
            //? if >=26.1 {
            GuiGraphicsExtractor graphics,
            //?} else {
            /*GuiGraphics graphics,
            *///?}
            int anchorX,
            int anchorY,
            int buttonX,
            int buttonY
    ) {
        int minX = Math.min(anchorX, buttonX);
        int maxX = Math.max(anchorX, buttonX);
        int minY = Math.min(anchorY, buttonY);
        int maxY = Math.max(anchorY, buttonY);
        ClientUiAdapter.fillRect(graphics, minX, anchorY, maxX + 1, anchorY + 1, COLOR_GUIDE_LINE);
        ClientUiAdapter.fillRect(graphics, buttonX, minY, buttonX + 1, maxY + 1, COLOR_GUIDE_LINE);
    }

    private boolean handlePreviewMouseClicked(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        int containerLeft = containerPreviewLeft();
        int containerTop = containerPreviewTop();
        int previewWidth = activePreviewButtonWidth();
        ContainerButtonPosition.ResolvedPosition resolved = resolveDraftPosition(containerLeft, containerTop, previewWidth);
        if (!isInsideRect(mouseX, mouseY, resolved.x(), resolved.y(), previewWidth, PREVIEW_BUTTON_HEIGHT)) {
            return false;
        }
        this.dragging = true;
        this.dragGrabOffsetX = (int) Math.round(mouseX) - resolved.x();
        this.dragGrabOffsetY = (int) Math.round(mouseY) - resolved.y();
        return true;
    }

    private boolean handlePreviewMouseDragged(double mouseX, double mouseY, int button) {
        if (!this.dragging || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        int targetX = (int) Math.round(mouseX) - this.dragGrabOffsetX;
        int targetY = (int) Math.round(mouseY) - this.dragGrabOffsetY;
        applyAbsoluteDraftPosition(targetX, targetY);
        return true;
    }

    private boolean handlePreviewMouseReleased(int button) {
        if (!this.dragging || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        this.dragging = false;
        return true;
    }

    private boolean handleNudgeKey(boolean left, boolean right, boolean up, boolean down, boolean shift) {
        int step = shift ? FAST_NUDGE_STEP_PX : NUDGE_STEP_PX;
        int dx = 0;
        int dy = 0;
        if (left) {
            dx -= step;
        } else if (right) {
            dx += step;
        } else if (up) {
            dy -= step;
        } else if (down) {
            dy += step;
        } else {
            return false;
        }

        int containerLeft = containerPreviewLeft();
        int containerTop = containerPreviewTop();
        int previewWidth = activePreviewButtonWidth();
        ContainerButtonPosition.ResolvedPosition current = resolveDraftPosition(containerLeft, containerTop, previewWidth);
        applyAbsoluteDraftPosition(current.x() + dx, current.y() + dy);
        return true;
    }

    private void applyAbsoluteDraftPosition(int absoluteX, int absoluteY) {
        ContainerButtonPosition updated = ContainerButtonPosition.fromAbsolutePosition(
                absoluteX,
                absoluteY,
                containerPreviewLeft(),
                containerPreviewTop(),
                CONTAINER_PREVIEW_WIDTH,
                this.width,
                this.height,
                activePreviewButtonWidth(),
                PREVIEW_BUTTON_HEIGHT
        );
        if (!updated.equals(this.draftPosition)) {
            this.draftPosition = updated;
            resetBannerToHintIfFeedback();
            refreshActionButtons();
        }
    }

    private void onSaveClicked() {
        if (!hasUnsavedChanges()) {
            return;
        }
        boolean saved = ClientContainerButtonConfigService.savePosition(this.draftPosition);
        if (saved) {
            this.committedPosition = this.draftPosition;
            this.statusMessage = AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_SAVED);
            this.statusColor = COLOR_BANNER_SUCCESS;
        } else {
            this.statusMessage = AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_SAVE_FAILED);
            this.statusColor = COLOR_BANNER_ERROR;
        }
        refreshActionButtons();
    }

    private void onRevertClicked() {
        if (!hasUnsavedChanges()) {
            return;
        }
        this.dragging = false;
        this.draftPosition = this.committedPosition;
        resetBannerToHintIfFeedback();
        refreshActionButtons();
    }

    private void onDefaultClicked() {
        if (isDefaultPosition()) {
            return;
        }
        this.dragging = false;
        this.draftPosition = ContainerButtonPosition.DEFAULT;
        resetBannerToHintIfFeedback();
        refreshActionButtons();
    }

    private void closeAndRevert() {
        this.dragging = false;
        this.draftPosition = this.committedPosition;
        if (this.minecraft != null) {
            ClientUiAdapter.showScreen(this.minecraft, this.parentScreen);
        }
    }

    private boolean hasUnsavedChanges() {
        return !this.draftPosition.equals(this.committedPosition);
    }

    private boolean isDefaultPosition() {
        return this.draftPosition.equals(ContainerButtonPosition.DEFAULT);
    }

    private void resetBannerToHintIfFeedback() {
        if (this.statusColor == COLOR_BANNER_SUCCESS || this.statusColor == COLOR_BANNER_ERROR) {
            this.statusMessage = AFKraftTranslations.translatable(AFKraftTranslations.GUI_BUTTON_POS_HINT);
            this.statusColor = COLOR_BANNER_INFO;
        }
    }

    private void refreshActionButtons() {
        boolean dirty = hasUnsavedChanges();
        if (this.saveButton != null) {
            this.saveButton.active = dirty;
        }
        if (this.revertButton != null) {
            this.revertButton.active = dirty;
        }
        if (this.defaultButton != null) {
            this.defaultButton.active = !isDefaultPosition();
        }
        if (this.cancelButton != null) {
            this.cancelButton.active = true;
        }
    }

    private Component previewModeToggleLabel() {
        return AFKraftTranslations.translatable(
                this.previewShulkerMode
                        ? AFKraftTranslations.GUI_BUTTON_POS_PREVIEW_SHULKER
                        : AFKraftTranslations.GUI_BUTTON_POS_PREVIEW_STANDARD
        );
    }

    private AbstractWidget activePreviewWidget() {
        return this.previewShulkerMode ? this.shulkerPreviewButton : this.standardPreviewButton;
    }

    private int activePreviewButtonWidth() {
        return this.previewShulkerMode ? SHULKER_BUTTON_WIDTH : STANDARD_BUTTON_WIDTH;
    }

    private int containerPreviewLeft() {
        return (this.width - CONTAINER_PREVIEW_WIDTH) / 2;
    }

    private int containerPreviewTop() {
        return (this.height - CONTAINER_PREVIEW_HEIGHT) / 2;
    }

    private ContainerButtonPosition.ResolvedPosition resolveDraftPosition(
            int containerLeft,
            int containerTop,
            int previewWidth
    ) {
        return this.draftPosition.resolveForContainer(
                containerLeft,
                containerTop,
                CONTAINER_PREVIEW_WIDTH,
                this.width,
                this.height,
                previewWidth,
                PREVIEW_BUTTON_HEIGHT
        );
    }

    private static boolean isInsideRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
