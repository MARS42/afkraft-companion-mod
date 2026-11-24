package com.mars4two.ikang.client.core.preferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerButtonConfigStoreTest {

    @Test
    void defaultPositionMatchesLegacyTopRightOffsetForCenteredContainer() {
        int screenWidth = 640;
        int screenHeight = 360;
        int containerWidth = 176;
        int containerLeft = (screenWidth - containerWidth) / 2; // 232
        int containerTop = (screenHeight - 166) / 2; // 97

        ContainerButtonPosition.ResolvedPosition resolved = ContainerButtonPosition.DEFAULT.resolveForContainer(
                containerLeft,
                containerTop,
                containerWidth,
                screenWidth,
                screenHeight,
                48,
                16
        );

        assertEquals(containerLeft + containerWidth + 4, resolved.x());
        assertEquals(screenWidth - containerLeft + 4, resolved.x());
        assertEquals(containerTop + 4, resolved.y());
    }

    @Test
    void resolveFollowsShiftedContainerAndClampsWithinScreenBounds() {
        ContainerButtonPosition farRight = new ContainerButtonPosition(500, -200);
        ContainerButtonPosition.ResolvedPosition clampedTopRight = farRight.resolveForContainer(
                300, 100, 176, 640, 360, 84, 16
        );
        assertEquals(640 - 84, clampedTopRight.x());
        assertEquals(0, clampedTopRight.y());

        ContainerButtonPosition farLeft = new ContainerButtonPosition(-900, 900);
        ContainerButtonPosition.ResolvedPosition clampedBottomLeft = farLeft.resolveForContainer(
                232, 97, 176, 640, 360, 48, 16
        );
        assertEquals(0, clampedBottomLeft.x());
        assertEquals(360 - 16, clampedBottomLeft.y());
    }

    @Test
    void fromAbsolutePositionRoundTripsWithResolveForContainer() {
        int containerLeft = 232;
        int containerTop = 97;
        int containerWidth = 176;
        int screenWidth = 640;
        int screenHeight = 360;

        ContainerButtonPosition computed = ContainerButtonPosition.fromAbsolutePosition(
                140,
                70,
                containerLeft,
                containerTop,
                containerWidth,
                screenWidth,
                screenHeight,
                84,
                16
        );

        assertEquals(-268, computed.offsetX());
        assertEquals(-27, computed.offsetY());

        ContainerButtonPosition.ResolvedPosition resolved = computed.resolveForContainer(
                containerLeft,
                containerTop,
                containerWidth,
                screenWidth,
                screenHeight,
                84,
                16
        );
        assertEquals(140, resolved.x());
        assertEquals(70, resolved.y());
    }

    @Test
    void loadReturnsDefaultWhenConfigFileDoesNotExist(@TempDir Path tempDir) {
        ContainerButtonConfigStore store = new ContainerButtonConfigStore(
                tempDir.resolve("config").resolve("afkraft_companion-client.json")
        );

        assertEquals(ContainerButtonPosition.DEFAULT, store.load());
    }

    @Test
    void saveCreatesParentDirectoriesAndRoundTripsOffsets(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("nested").resolve("config").resolve("afkraft_companion-client.json");
        ContainerButtonConfigStore store = new ContainerButtonConfigStore(configFile);

        ContainerButtonPosition custom = new ContainerButtonPosition(-210, 42);
        store.save(custom);

        assertTrue(Files.isRegularFile(configFile));
        assertEquals(custom, store.load());
    }

    @Test
    void loadFallsBackToDefaultOnCorruptedOrIncompleteJson(@TempDir Path tempDir) throws IOException {
        Path brokenFile = tempDir.resolve("broken.json");
        Files.writeString(brokenFile, "{not valid json", StandardCharsets.UTF_8);
        assertEquals(ContainerButtonPosition.DEFAULT, new ContainerButtonConfigStore(brokenFile).load());

        Path incompleteFile = tempDir.resolve("incomplete.json");
        Files.writeString(incompleteFile, "{\"version\":1,\"offsetX\":10}", StandardCharsets.UTF_8);
        assertEquals(ContainerButtonPosition.DEFAULT, new ContainerButtonConfigStore(incompleteFile).load());

        Path blankFile = tempDir.resolve("blank.json");
        Files.writeString(blankFile, "   \n ", StandardCharsets.UTF_8);
        assertEquals(ContainerButtonPosition.DEFAULT, new ContainerButtonConfigStore(blankFile).load());
    }
}
