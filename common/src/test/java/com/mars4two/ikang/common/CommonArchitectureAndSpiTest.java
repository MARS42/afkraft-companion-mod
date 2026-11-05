package com.mars4two.ikang.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonArchitectureAndSpiTest {

    private static final Path COMMON_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path REPO_ROOT = COMMON_ROOT.getParent();
    private static final Path MAIN_JAVA = COMMON_ROOT.resolve("src/main/java");
    private static final Path CLIENT_JAVA = COMMON_ROOT.resolve("src/client/java");

    @Test
    void commonMainAndClientContainZeroModLoaderImports() throws IOException {
        assertTrue(Files.isDirectory(MAIN_JAVA), "Expected common/src/main/java to exist");
        assertTrue(Files.isDirectory(CLIENT_JAVA), "Expected common/src/client/java to exist");

        List<Path> allJava = new ArrayList<>();
        allJava.addAll(collectJavaFiles(MAIN_JAVA));
        allJava.addAll(collectJavaFiles(CLIENT_JAVA));
        assertFalse(allJava.isEmpty(), "Expected common Java sources to be non-empty");

        List<String> forbiddenPrefixes = List.of(
                "net.fabricmc.",
                "net.neoforged.",
                "net.minecraftforge."
        );

        List<String> violations = new ArrayList<>();
        for (Path file : allJava) {
            String content = Files.readString(file);
            for (String forbidden : forbiddenPrefixes) {
                if (content.contains(forbidden)) {
                    violations.add(COMMON_ROOT.relativize(file) + " references " + forbidden);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "Loader leakage in :common:\n" + String.join("\n", violations));
    }

    @Test
    void commonMainContainsZeroClientMinecraftImports() throws IOException {
        List<Path> mainJava = collectJavaFiles(MAIN_JAVA);
        assertFalse(mainJava.isEmpty(), "Expected common/src/main/java sources to be non-empty");

        List<String> forbiddenClientPrefixes = List.of(
                "net.minecraft.client.",
                "com.mojang.blaze3d.",
                "com.mars4two.ikang.client."
        );

        List<String> violations = new ArrayList<>();
        for (Path file : mainJava) {
            String content = Files.readString(file);
            for (String forbidden : forbiddenClientPrefixes) {
                if (content.contains(forbidden)) {
                    violations.add(COMMON_ROOT.relativize(file) + " references " + forbidden);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "Client leakage in common/src/main:\n" + String.join("\n", violations));
    }

    @Test
    void playerSnapshotMixinReplacesRawAttackOverrideWithInject() throws IOException {
        Path playerSnapshotMixin = MAIN_JAVA.resolve("com/mars4two/ikang/mixin/PlayerSnapshotMixin.java");
        Path playerAttackMixin = MAIN_JAVA.resolve("com/mars4two/ikang/mixin/PlayerAttackMixin.java");

        assertTrue(Files.isRegularFile(playerSnapshotMixin), "Missing PlayerSnapshotMixin.java");
        assertTrue(Files.isRegularFile(playerAttackMixin), "Missing PlayerAttackMixin.java");

        String snapshotSource = Files.readString(playerSnapshotMixin);
        assertFalse(
                snapshotSource.contains("public void attack("),
                "PlayerSnapshotMixin must not override vanilla attack(Entity) directly"
        );

        String attackSource = Files.readString(playerAttackMixin);
        assertTrue(
                attackSource.contains("@Inject") && attackSource.contains("method = \"attack\""),
                "PlayerAttackMixin must use @Inject on Player.attack"
        );
    }

    @Test
    void allFiveSpisAndFiveVersionAdaptersExist() {
        List<String> requiredPaths = List.of(
                "src/main/java/com/mars4two/ikang/platform/services/Services.java",
                "src/main/java/com/mars4two/ikang/platform/services/NetworkBridge.java",
                "src/main/java/com/mars4two/ikang/platform/services/ServerLifecycleHooks.java",
                "src/main/java/com/mars4two/ikang/platform/services/BlockInteractionHooks.java",
                "src/client/java/com/mars4two/ikang/client/platform/services/ClientServices.java",
                "src/client/java/com/mars4two/ikang/client/platform/services/ClientCommandBridge.java",
                "src/client/java/com/mars4two/ikang/client/platform/services/ScreenWidgetBridge.java",
                "src/main/java/com/mars4two/ikang/platform/mc/SavedDataAdapter.java",
                "src/main/java/com/mars4two/ikang/platform/mc/NbtCompatAdapter.java",
                "src/main/java/com/mars4two/ikang/platform/mc/InventoryEquipmentAdapter.java",
                "src/main/java/com/mars4two/ikang/platform/mc/WorldAndEntityAdapter.java",
                "src/client/java/com/mars4two/ikang/client/platform/mc/ClientUiAdapter.java"
        );

        for (String rel : requiredPaths) {
            Path target = COMMON_ROOT.resolve(rel);
            assertTrue(Files.isRegularFile(target), "Expected file to exist in :common: " + rel);
        }
    }

    @Test
    void fabricServiceProviderDescriptorsMatchSpiContracts() throws IOException {
        Path fabricRoot = REPO_ROOT.resolve("fabric");
        List<ServiceBinding> bindings = List.of(
                new ServiceBinding(
                        "src/main/resources/META-INF/services/com.mars4two.ikang.platform.services.NetworkBridge",
                        "src/main/java"
                ),
                new ServiceBinding(
                        "src/main/resources/META-INF/services/com.mars4two.ikang.platform.services.ServerLifecycleHooks",
                        "src/main/java"
                ),
                new ServiceBinding(
                        "src/main/resources/META-INF/services/com.mars4two.ikang.platform.services.BlockInteractionHooks",
                        "src/main/java"
                ),
                new ServiceBinding(
                        "src/client/resources/META-INF/services/com.mars4two.ikang.client.platform.services.ClientCommandBridge",
                        "src/client/java"
                ),
                new ServiceBinding(
                        "src/client/resources/META-INF/services/com.mars4two.ikang.client.platform.services.ScreenWidgetBridge",
                        "src/client/java"
                )
        );

        for (ServiceBinding binding : bindings) {
            Path descriptor = fabricRoot.resolve(binding.descriptorPath());
            assertTrue(Files.isRegularFile(descriptor), "Missing ServiceLoader descriptor: " + descriptor);
            String implFqcn = Files.readString(descriptor).trim();
            assertFalse(implFqcn.isEmpty(), "Empty ServiceLoader descriptor: " + descriptor);
            Path implJava = fabricRoot.resolve(binding.sourceRoot())
                    .resolve(implFqcn.replace('.', '/') + ".java");
            assertTrue(Files.isRegularFile(implJava), "Missing Fabric SPI implementation source: " + implJava);
        }
    }

    @Test
    void compiledCommonBytecodeContainsZeroLoaderOrClientLeakage() throws IOException {
        Path mainClassesDir = COMMON_ROOT.resolve("build/classes/java/main");
        Path clientClassesDir = COMMON_ROOT.resolve("build/classes/java/client");
        assertTrue(Files.isDirectory(mainClassesDir), "Expected compiled main classes directory");
        assertTrue(Files.isDirectory(clientClassesDir), "Expected compiled client classes directory");

        List<Path> mainClasses = collectFilesWithSuffix(mainClassesDir, ".class");
        List<Path> clientClasses = collectFilesWithSuffix(clientClassesDir, ".class");
        assertFalse(mainClasses.isEmpty(), "Expected compiled main classes");
        assertFalse(clientClasses.isEmpty(), "Expected compiled client classes");

        List<String> forbiddenLoaderInternalNames = List.of(
                "net/fabricmc/",
                "net/neoforged/",
                "net/minecraftforge/"
        );
        List<String> forbiddenClientInternalNames = List.of(
                "net/minecraft/client/",
                "com/mojang/blaze3d/",
                "com/mars4two/ikang/client/"
        );

        List<String> violations = new ArrayList<>();
        for (Path cls : mainClasses) {
            String latin1 = new String(Files.readAllBytes(cls), java.nio.charset.StandardCharsets.ISO_8859_1);
            for (String token : forbiddenLoaderInternalNames) {
                if (latin1.contains(token)) {
                    violations.add("main bytecode " + mainClassesDir.relativize(cls) + " references " + token);
                }
            }
            for (String token : forbiddenClientInternalNames) {
                if (latin1.contains(token)) {
                    violations.add("main bytecode " + mainClassesDir.relativize(cls) + " references client type " + token);
                }
            }
        }
        for (Path cls : clientClasses) {
            String latin1 = new String(Files.readAllBytes(cls), java.nio.charset.StandardCharsets.ISO_8859_1);
            for (String token : forbiddenLoaderInternalNames) {
                if (latin1.contains(token)) {
                    violations.add("client bytecode " + clientClassesDir.relativize(cls) + " references " + token);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "Bytecode leakage in :common:\n" + String.join("\n", violations));
    }

    private static List<Path> collectJavaFiles(Path root) throws IOException {
        return collectFilesWithSuffix(root, ".java");
    }

    private static List<Path> collectFilesWithSuffix(Path root, String suffix) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(suffix))
                    .toList();
        }
    }

    private record ServiceBinding(String descriptorPath, String sourceRoot) {
    }
}
