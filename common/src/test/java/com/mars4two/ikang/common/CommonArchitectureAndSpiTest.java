package com.mars4two.ikang.common;

import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonArchitectureAndSpiTest {

    private static final Path NODE_OR_MODULE_DIR = Path.of(System.getProperty("user.dir"));
    private static final Path COMMON_ROOT = resolveCommonBranchRoot(NODE_OR_MODULE_DIR);
    private static final Path REPO_ROOT = COMMON_ROOT.getParent();
    private static final Path MAIN_JAVA = COMMON_ROOT.resolve("src/main/java");
    private static final Path CLIENT_JAVA = COMMON_ROOT.resolve("src/client/java");
    private static final Path BUILD_DIR = NODE_OR_MODULE_DIR.resolve("build");

    private static Path resolveCommonBranchRoot(Path workingDir) {
        String explicit = System.getProperty("afkraft.commonBranchRoot");
        if (explicit != null && !explicit.isBlank()) {
            return Path.of(explicit);
        }
        if (workingDir.getParent() != null
                && "versions".equals(workingDir.getParent().getFileName().toString())
                && workingDir.getParent().getParent() != null) {
            return workingDir.getParent().getParent();
        }
        return workingDir;
    }

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
                "com.mojang.renderpearl.",
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
    void allSpisAndFiveVersionAdaptersExist() {
        List<String> requiredPaths = List.of(
                "src/main/java/com/mars4two/ikang/platform/services/Services.java",
                "src/main/java/com/mars4two/ikang/platform/services/NetworkBridge.java",
                "src/main/java/com/mars4two/ikang/platform/services/ServerLifecycleHooks.java",
                "src/main/java/com/mars4two/ikang/platform/services/BlockInteractionHooks.java",
                "src/main/java/com/mars4two/ikang/platform/services/PlatformInfoHooks.java",
                "src/client/java/com/mars4two/ikang/client/platform/services/ClientServices.java",
                "src/client/java/com/mars4two/ikang/client/platform/services/ClientCommandBridge.java",
                "src/client/java/com/mars4two/ikang/client/platform/services/ScreenWidgetBridge.java",
                "src/client/java/com/mars4two/ikang/client/platform/services/ClientLifecycleHooks.java",
                "src/client/java/com/mars4two/ikang/client/platform/services/ClientKeyMappingBridge.java",
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
                ),
                new ServiceBinding(
                        "src/main/resources/META-INF/services/com.mars4two.ikang.platform.services.PlatformInfoHooks",
                        "src/main/java"
                ),
                new ServiceBinding(
                        "src/client/resources/META-INF/services/com.mars4two.ikang.client.platform.services.ClientLifecycleHooks",
                        "src/client/java"
                ),
                new ServiceBinding(
                        "src/client/resources/META-INF/services/com.mars4two.ikang.client.platform.services.ClientKeyMappingBridge",
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
    void neoforgeServiceProviderDescriptorsAndMetadataMatchSpiContracts() throws IOException {
        Path neoforgeRoot = REPO_ROOT.resolve("neoforge");
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
                ),
                new ServiceBinding(
                        "src/main/resources/META-INF/services/com.mars4two.ikang.platform.services.PlatformInfoHooks",
                        "src/main/java"
                ),
                new ServiceBinding(
                        "src/client/resources/META-INF/services/com.mars4two.ikang.client.platform.services.ClientLifecycleHooks",
                        "src/client/java"
                ),
                new ServiceBinding(
                        "src/client/resources/META-INF/services/com.mars4two.ikang.client.platform.services.ClientKeyMappingBridge",
                        "src/client/java"
                )
        );

        for (ServiceBinding binding : bindings) {
            Path descriptor = neoforgeRoot.resolve(binding.descriptorPath());
            assertTrue(Files.isRegularFile(descriptor), "Missing NeoForge ServiceLoader descriptor: " + descriptor);
            String implFqcn = Files.readString(descriptor).trim();
            assertFalse(implFqcn.isEmpty(), "Empty NeoForge ServiceLoader descriptor: " + descriptor);
            Path implJava = neoforgeRoot.resolve(binding.sourceRoot())
                    .resolve(implFqcn.replace('.', '/') + ".java");
            assertTrue(Files.isRegularFile(implJava), "Missing NeoForge SPI implementation source: " + implJava);
        }

        Path modsToml = neoforgeRoot.resolve("src/main/resources/META-INF/neoforge.mods.toml");
        assertTrue(Files.isRegularFile(modsToml), "Missing META-INF/neoforge.mods.toml");
        String tomlContent = Files.readString(modsToml);
        assertTrue(tomlContent.contains("modId=\"afkraft_companion\""), "neoforge.mods.toml must declare modId=\"afkraft_companion\"");
        assertTrue(tomlContent.contains("afkraft_companion.mixins.json"), "neoforge.mods.toml must register server mixin config");
        assertTrue(tomlContent.contains("afkraft_companion.client.mixins.json"), "neoforge.mods.toml must register client mixin config");
        assertTrue(tomlContent.contains("updateJSONURL="), "neoforge.mods.toml must declare updateJSONURL");
    }

    @Test
    void compiledCommonBytecodeContainsZeroLoaderOrClientLeakage() throws IOException {
        Path mainClassesDir = BUILD_DIR.resolve("classes/java/main");
        Path clientClassesDir = BUILD_DIR.resolve("classes/java/client");
        assertTrue(Files.isDirectory(mainClassesDir), "Expected compiled main classes directory: " + mainClassesDir);
        assertTrue(Files.isDirectory(clientClassesDir), "Expected compiled client classes directory: " + clientClassesDir);

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
                "com/mojang/renderpearl/",
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

    @Test
    void everyClientboundPayloadRegistrationHasMatchingClientReceiver() throws IOException {
        Path payloadRegister = MAIN_JAVA.resolve("com/mars4two/ikang/network/PayloadRegister.java");
        Path clientEntry = CLIENT_JAVA.resolve("com/mars4two/ikang/client/AFKraftCompanionClient.java");
        assertTrue(Files.isRegularFile(payloadRegister), "Missing PayloadRegister.java");
        assertTrue(Files.isRegularFile(clientEntry), "Missing AFKraftCompanionClient.java");

        String registerSource = Files.readString(payloadRegister);
        String clientSource = Files.readString(clientEntry);

        assertFalse(
                registerSource.contains("ContainerData.PAYLOAD_TYPE"),
                "PayloadRegister must not register nested ContainerData as a top-level clientbound packet"
        );
        assertEquals(
                countOccurrences(registerSource, ".registerClientbound("),
                countOccurrences(clientSource, ".registerClientReceiver("),
                "Every registered clientbound payload in PayloadRegister must have a client receiver in AFKraftCompanionClient"
        );
    }

    @Test
    void stonecutterEndpointsIdeRunsCiAndVersioningGuideContract() throws IOException {
        Path stonecutterGradle = REPO_ROOT.resolve("stonecutter.gradle");
        assertTrue(Files.isRegularFile(stonecutterGradle), "Missing root stonecutter.gradle controller");
        String controllerContent = Files.readString(stonecutterGradle);
        assertTrue(controllerContent.contains("stonecutter.active"), "stonecutter.gradle must declare active project");
        assertTrue(controllerContent.contains("chiseledBuild"), "stonecutter.gradle must register chiseledBuild");

        List<String> requiredPropsKeys = List.of(
                "minecraft_version=",
                "java_version=",
                "mixin_compat_level=",
                "neoform_version=",
                "neoforge_version=",
                "fabric_api_version=",
                "fabric_mc_range=",
                "neoforge_mc_range=",
                "neoforge_loader_range="
        );
        for (String endpoint : List.of("1.21.1", "26.2", "26.3")) {
            Path propsPath = REPO_ROOT.resolve("versions").resolve(endpoint).resolve("gradle.properties");
            assertTrue(Files.isRegularFile(propsPath), "Missing endpoint properties: " + propsPath);
            String propsContent = Files.readString(propsPath);
            for (String key : requiredPropsKeys) {
                assertTrue(propsContent.contains(key), "Missing key " + key + " in " + propsPath);
            }
        }

        String nodeName = NODE_OR_MODULE_DIR.getFileName().toString();
        int expectedClassMajor = nodeName.startsWith("1.21") ? 65 : 69;
        for (Path classesDir : List.of(BUILD_DIR.resolve("classes/java/main"), BUILD_DIR.resolve("classes/java/client"))) {
            for (Path cls : collectFilesWithSuffix(classesDir, ".class")) {
                byte[] bytes = Files.readAllBytes(cls);
                int major = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
                assertEquals(
                        expectedClassMajor,
                        major,
                        () -> "Unexpected bytecode major version for node " + nodeName + " in " + classesDir.relativize(cls)
                );
            }
        }

        String fabricBuild = Files.readString(REPO_ROOT.resolve("fabric/build.gradle"));
        assertTrue(fabricBuild.contains("ideConfigGenerated(true)"), "fabric/build.gradle must enable ideConfigGenerated(true)");
        assertTrue(fabricBuild.contains("net.fabricmc.fabric-loom-remap"), "fabric/build.gradle must support obfuscated loom-remap");
        assertTrue(fabricBuild.contains("net.fabricmc.fabric-loom"), "fabric/build.gradle must support unobfuscated loom");

        String neoforgeBuild = Files.readString(REPO_ROOT.resolve("neoforge/build.gradle"));
        assertTrue(neoforgeBuild.contains("runs {"), "neoforge/build.gradle must configure IDE runs block");

        assertFalse(
                Files.exists(REPO_ROOT.resolve(".idea/runConfigurations/Minecraft_Client.xml")),
                "Legacy single-module Minecraft_Client.xml run config must be removed"
        );
        assertFalse(
                Files.exists(REPO_ROOT.resolve(".idea/runConfigurations/Minecraft_Server.xml")),
                "Legacy single-module Minecraft_Server.xml run config must be removed"
        );

        String workflowYaml = Files.readString(REPO_ROOT.resolve(".github/workflows/build.yml"));
        assertTrue(workflowYaml.contains("21") && workflowYaml.contains("25"), "CI workflow must provision Java 21 and Java 25");

        Path versioningGuide = REPO_ROOT.resolve("VERSIONING.md");
        assertTrue(Files.isRegularFile(versioningGuide), "Missing root VERSIONING.md maintainer guide");
        String guideContent = Files.readString(versioningGuide);
        assertTrue(guideContent.contains("versions/") && guideContent.contains("chiseledBuild"), "VERSIONING.md must document version onboarding and chiseledBuild");
    }

    @Test
    void blockItemMixinUsesVersionAwareServerPlacementResultPredicate() throws IOException {
        Path blockItemMixin = MAIN_JAVA.resolve("com/mars4two/ikang/mixin/BlockItemMixin.java");
        assertTrue(Files.isRegularFile(blockItemMixin), "Missing BlockItemMixin.java");
        String mixinSource = Files.readString(blockItemMixin);
        assertFalse(
                mixinSource.contains("!= InteractionResult.SUCCESS"),
                "BlockItemMixin must not hardcode != InteractionResult.SUCCESS because 1.21.1 BlockItem.place returns CONSUME on server"
        );
        assertTrue(
                mixinSource.contains("WorldAndEntityAdapter.isSuccessfulBlockPlacement("),
                "BlockItemMixin must delegate placement result check to WorldAndEntityAdapter.isSuccessfulBlockPlacement"
        );

        assertFalse(WorldAndEntityAdapter.isSuccessfulBlockPlacement(null));
        assertFalse(WorldAndEntityAdapter.isSuccessfulBlockPlacement(InteractionResult.PASS));
        assertFalse(WorldAndEntityAdapter.isSuccessfulBlockPlacement(InteractionResult.FAIL));
        assertTrue(WorldAndEntityAdapter.isSuccessfulBlockPlacement(InteractionResult.SUCCESS));
        //? if >=1.21.2 {
        assertFalse(WorldAndEntityAdapter.isSuccessfulBlockPlacement(InteractionResult.TRY_WITH_EMPTY_HAND));
        //?} else {
        /*assertTrue(
                WorldAndEntityAdapter.isSuccessfulBlockPlacement(InteractionResult.sidedSuccess(false)),
                "1.21.1 server-side BlockItem.place returns sidedSuccess(false) == CONSUME and must be treated as successful placement"
        );
        assertFalse(WorldAndEntityAdapter.isSuccessfulBlockPlacement(InteractionResult.CONSUME_PARTIAL));
        assertFalse(WorldAndEntityAdapter.isSuccessfulBlockPlacement(InteractionResult.SUCCESS_NO_ITEM_USED));
        *///?}
    }

    @Test
    void hotbarPickupItemDurabilityAndEquipmentHurtMixinsAreRegisteredAndWired() throws IOException {
        Path mixinsJson = COMMON_ROOT.resolve("src/main/resources/afkraft_companion.mixins.json");
        Path itemStackMixin = MAIN_JAVA.resolve("com/mars4two/ikang/mixin/ItemStackMixin.java");
        Path playerInventoryMixin = MAIN_JAVA.resolve("com/mars4two/ikang/mixin/PlayerInventoryMixin.java");
        Path playerSnapshotMixin = MAIN_JAVA.resolve("com/mars4two/ikang/mixin/PlayerSnapshotMixin.java");

        assertTrue(Files.isRegularFile(mixinsJson), "Missing afkraft_companion.mixins.json");
        assertTrue(Files.isRegularFile(itemStackMixin), "Missing ItemStackMixin.java");
        assertTrue(Files.isRegularFile(playerInventoryMixin), "Missing PlayerInventoryMixin.java");
        assertTrue(Files.isRegularFile(playerSnapshotMixin), "Missing PlayerSnapshotMixin.java");

        String mixinsConfig = Files.readString(mixinsJson);
        assertTrue(mixinsConfig.contains("\"ItemStackMixin\""), "afkraft_companion.mixins.json must register ItemStackMixin");

        String itemStackSource = Files.readString(itemStackMixin);
        assertTrue(itemStackSource.contains("hurtAndBreak"), "ItemStackMixin must inject into hurtAndBreak");
        assertTrue(itemStackSource.contains("afkraft_companion$syncEquipmentAndHotbar"), "ItemStackMixin must trigger syncEquipmentAndHotbar");

        String inventorySource = Files.readString(playerInventoryMixin);
        assertTrue(
                inventorySource.contains("ingestPlayerFullInventoryState"),
                "PlayerInventoryMixin must ingest full inventory state (hotbar + inventory + equipment) on non-slot-specific additions/updates"
        );

        String snapshotSource = Files.readString(playerSnapshotMixin);
        assertTrue(
                snapshotSource.contains("afkraft_companion$syncEquipmentAndHotbar"),
                "PlayerSnapshotMixin hurt handler must trigger syncEquipmentAndHotbar"
        );
        assertTrue(
                snapshotSource.contains("afkraft_companion$syncSlotChange"),
                "PlayerSnapshotMixin initMenu listener must sync slot changes"
        );
    }

    @Test
    void qrModalScreenSupportsThreeStateCopyUrlAndQrFlowViaClientUiAdapter() throws IOException {
        Path qrModal = CLIENT_JAVA.resolve("com/mars4two/ikang/client/core/screens/QRModalScreen.java");
        Path uiAdapter = CLIENT_JAVA.resolve("com/mars4two/ikang/client/platform/mc/ClientUiAdapter.java");

        assertTrue(Files.isRegularFile(qrModal), "Missing QRModalScreen.java");
        assertTrue(Files.isRegularFile(uiAdapter), "Missing ClientUiAdapter.java");

        String modalSource = Files.readString(qrModal);
        assertTrue(modalSource.contains("INITIAL_WARNING"), "QRModalScreen must define INITIAL_WARNING state");
        assertTrue(modalSource.contains("SHOW_QR"), "QRModalScreen must define SHOW_QR state");
        assertTrue(modalSource.contains("URL_COPIED"), "QRModalScreen must define URL_COPIED state");
        assertTrue(modalSource.contains("ClientUiAdapter.setClipboard("), "QRModalScreen must copy URL via ClientUiAdapter.setClipboard");
        assertTrue(modalSource.contains("AFKraftTranslations.GUI_BTN_COPY_URL"), "QRModalScreen must use GUI_BTN_COPY_URL translation key");
        assertTrue(modalSource.contains("AFKraftTranslations.GUI_BTN_USE_QR"), "QRModalScreen must use GUI_BTN_USE_QR translation key");
        assertTrue(modalSource.contains("AFKraftTranslations.GUI_CONNECT_QR_URL_COPIED"), "QRModalScreen must use GUI_CONNECT_QR_URL_COPIED translation key");
        assertFalse(modalSource.contains("keyboardHandler"), "QRModalScreen must not touch Minecraft.keyboardHandler directly; delegate to ClientUiAdapter");

        String adapterSource = Files.readString(uiAdapter);
        assertTrue(adapterSource.contains("public static void setClipboard("), "ClientUiAdapter must expose setClipboard helper");
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
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
