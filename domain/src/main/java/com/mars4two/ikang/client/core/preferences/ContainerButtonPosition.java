package com.mars4two.ikang.client.core.preferences;

/**
 * Pure-domain offset for the AFK container toggle button, anchored to the container GUI's
 * top-right corner ({@code containerLeft + containerWidth}, {@code containerTop}).
 */
public record ContainerButtonPosition(int offsetX, int offsetY) {

    public static final int DEFAULT_OFFSET_X = 4;
    public static final int DEFAULT_OFFSET_Y = 4;
    public static final ContainerButtonPosition DEFAULT =
            new ContainerButtonPosition(DEFAULT_OFFSET_X, DEFAULT_OFFSET_Y);

    public record ResolvedPosition(int x, int y) {
    }

    public ResolvedPosition resolveForContainer(
            int containerLeft,
            int containerTop,
            int containerWidth,
            int screenWidth,
            int screenHeight,
            int buttonWidth,
            int buttonHeight
    ) {
        int rawX = containerLeft + containerWidth + this.offsetX;
        int rawY = containerTop + this.offsetY;
        return new ResolvedPosition(
                clamp(rawX, 0, Math.max(0, screenWidth - buttonWidth)),
                clamp(rawY, 0, Math.max(0, screenHeight - buttonHeight))
        );
    }

    public static ContainerButtonPosition fromAbsolutePosition(
            int buttonX,
            int buttonY,
            int containerLeft,
            int containerTop,
            int containerWidth,
            int screenWidth,
            int screenHeight,
            int buttonWidth,
            int buttonHeight
    ) {
        int clampedX = clamp(buttonX, 0, Math.max(0, screenWidth - buttonWidth));
        int clampedY = clamp(buttonY, 0, Math.max(0, screenHeight - buttonHeight));
        return new ContainerButtonPosition(
                clampedX - (containerLeft + containerWidth),
                clampedY - containerTop
        );
    }

    public ContainerButtonPosition translated(int deltaX, int deltaY) {
        return new ContainerButtonPosition(this.offsetX + deltaX, this.offsetY + deltaY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
