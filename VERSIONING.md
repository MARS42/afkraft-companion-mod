# Multi-Version & Multi-Loader Maintainer Guide

`afkraft-companion-mod` builds **Fabric** and **NeoForge** jars across **16 Minecraft releases (`1.21.1` through `26.3`)** from a single codebase using **Stonecutter** (`0.9.8`), a version-agnostic pure-Java `:domain` module, and isolated Minecraft version adapters in `:common`.

---

## Supported Version Nodes Matrix

Instead of duplicating a build node for every minor patch when Mojang's bytecode is binary-compatible for all symbols the mod touches, the build uses **10 Stonecutter compatibility-cluster nodes** (producing 20 release JARs):

| Stonecutter Node | Java Target | Fabric `minecraft` Range (Covers) | NeoForge `minecraft` Range (Stable Releases) | Notes |
|------------------|-------------|-----------------------------------|----------------------------------------------|-------|
| `1.21.1` | Java 21 | `~1.21` (`1.21`, `1.21.1`) | `[1.21.1,1.21.2)` (`1.21.1`) | Baseline 1.21 node |
| `1.21.3` | Java 21 | `>=1.21.2 <1.21.4` (`1.21.2`, `1.21.3`) | `[1.21.3,1.21.4)` (`1.21.3`) | NeoForge `1.21.2` had no stable release |
| `1.21.4` | Java 21 | `~1.21.4` (`1.21.4`) | `[1.21.4,1.21.5)` (`1.21.4`) | `PlayerInventory.addAndPickItem`, `ServerPlayer.drop` void |
| `1.21.5` | Java 21 | `~1.21.5` (`1.21.5`) | `[1.21.5,1.21.6)` (`1.21.5`) | `Optional` NBT, `SavedDataType(String, ...)`, `preRemoveSideEffects` |
| `1.21.8` | Java 21 | `>=1.21.6 <1.21.9` (`1.21.6`–`1.21.8`) | `[1.21.8,1.21.9)` (`1.21.8`) | `RenderPipelines.GUI_TEXTURED`; NeoForge `1.21.6`/`1.21.7` were beta-only |
| `1.21.10` | Java 21 | `>=1.21.9 <1.21.11` (`1.21.9`, `1.21.10`) | `[1.21.10,1.21.11)` (`1.21.10`) | `GameProfile` record (`authlib 7`), `KeyEvent`, `RespawnData`; NeoForge `1.21.9` beta-only |
| `1.21.11` | Java 21 | `~1.21.11` (`1.21.11`) | `[1.21.11,1.21.12)` (`1.21.11`) | `ResourceLocation` -> `Identifier`, `PermissionSet`, 3-arg `CycleButton.booleanBuilder` |
| `26.1.2` | Java 25 | `>=26.1 <26.2` (`26.1`–`26.1.2`) | `[26.1.2,26.2)` (`26.1.2`) | Unobfuscated Mojmap, `GuiGraphicsExtractor`, `SavedDataType(Identifier, ...)` |
| `26.2` | Java 25 | `~26.2` (`26.2`) | `[26.2,26.3)` (`26.2`) | `BlockEvents.USE_WITHOUT_ITEM`, `BreakBlockEvent` |
| `26.3` | Java 25 | `~26.3` (`26.3`) | `[26.3,26.4)` (`26.3`) | Active VCS version (`vcs_version`); `ServerboundPunchPacket`, `swingAndResetAttackStrength`, `renderpearl` `RenderPipeline` |

---

## New Minecraft Release Runbook (Fast-Track Playbook)

When Mojang releases a new Minecraft version (e.g., a hotfix `26.3.1` or a new Game Drop `26.4`), follow this 4-stage runbook to ship a verified release as fast as possible.

### Stage 1: Upstream Readiness Check (Pre-Flight)

Before creating or editing files, verify the 4 upstream coordinates for the new target `<NEW_VER>`:

| Upstream Dependency | Where to Query | What to Extract |
|---------------------|----------------|-----------------|
| **Minecraft Manifest** | `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` -> `<NEW_VER>.json` | `javaVersion.majorVersion` (e.g. `25`), whether `client_mappings` is absent (unobfuscated), and bundled `com.mojang:authlib` / `brigadier` versions. |
| **NeoForm** | `https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoform` | Latest `<NEW_VER>-<build>` (e.g., `26.3-1`). Required by `:common` and `:neoforge`. |
| **NeoForge** | `https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge` | Latest stable release for `<NEW_VER>` (e.g., `26.3.0.101`). If NeoForge only has `-beta` for a minor patch that is binary-compatible on Fabric, wait for stable NeoForge or widen only `fabric_mc_range`. |
| **Fabric Loader, Loom & API** | `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml` | Latest `<fapi_ver>+<NEW_VER>` (e.g., `0.165.2+26.3`). Check root `gradle.properties` if `loader_version` or `loom_version` needs a bump. |

### Stage 2: Binary Compatibility Triage (Range Extension vs. New Node)

Determine whether `<NEW_VER>` can share the existing top node's compiled JAR or requires a new `versions/<NEW_VER>/gradle.properties` node:

1. **Check every Mixin target & bytecode descriptor** in the new client/server JAR against the previous top version.
2. **Watch out for hidden bytecode descriptor drift**: Even when our Java source line does not change, a dependency type in a method descriptor may have moved packages (for example, in `26.3`, `RenderPipelines.GUI_TEXTURED` and `GuiGraphicsExtractor.blit(RenderPipeline, ...)` moved `RenderPipeline` from `com.mojang.blaze3d.pipeline` to `com.mojang.renderpearl.api.pipeline`). If any referenced method or field descriptor changed in bytecode, `<NEW_VER>` **must** have its own compiled node.
3. **Decision**:
   - **Zero bytecode/Mixin/loader diffs** (typical for hotfix patches like `26.1.1` -> `26.1.2`): Do **not** add a new Stonecutter node if one already covers the cluster; widen `fabric_mc_range` and `neoforge_mc_range` in the existing node's `gradle.properties` and run `./gradlew chiseledBuild`.
   - **Any Mixin target, method descriptor, or loader API changed** (typical for Game Drops like `26.2` -> `26.3`): Proceed to **Stage 3 & Stage 4** to add a new top node.

### Stage 3: The 6 Breaking-Change Inspection Checkpoints

When a new version fails compilation, Mixin application, or runtime tests, inspect these 6 areas in order:

| # | Checkpoint | Files to Inspect | Typical Mojang / Loader Breakage |
|---|------------|------------------|----------------------------------|
| **1** | **Mixin Targets & Hierarchy** | `common/src/main/java/com/mars4two/ikang/mixin/*.java` | Target method renamed, removed, or moved to a superclass/packet handler (e.g., `ServerPlayer.swing(InteractionHand)` in `<=26.2` split into `ServerGamePacketListenerImpl.handlePunch` + `ServerPlayer.swingAndResetAttackStrength` in `26.3`; `BlockEntity.preRemoveSideEffects` in `>=1.21.5`). Never override vanilla methods directly in Mixins; use `@Inject` + hook interfaces. |
| **2** | **World, Entity & Packet APIs** | `WorldAndEntityAdapter.java` | `ServerPlayer` level accessors, `EquipmentSlot` IDs, `InteractionResult` subtypes (`Success` record, `SwingSource`), `ClickEvent`/`HoverEvent` records, `LevelData.RespawnData`. |
| **3** | **Client UI, Rendering & Input** | `ClientUiAdapter.java`, `QRModalScreen.java`, `AuthorizeServerRequestScreen.java`, `ClientEvents.java` | `GuiGraphicsExtractor` method signatures (`blit`, `centeredText`, `outline`), `RenderPipelines` package moves (`blaze3d` -> `renderpearl`), `NativeImage` / `DynamicTexture`, `KeyEvent`, `CycleButton.booleanBuilder`, `PermissionSet` (`Permissions.COMMANDS_ADMIN`). |
| **4** | **Persistence, NBT & Inventory** | `SavedDataAdapter.java`, `NbtCompatAdapter.java`, `InventoryEquipmentAdapter.java`, `AFKContainerConfigData.java` | `SavedDataType` constructors (`Identifier` vs `String`), `SavedDataStorage` vs `DimensionDataStorage`, `CompoundTag` `Optional` getters, `Inventory` slot selection & `DataComponents`. |
| **5** | **Loader SPIs (Fabric & NeoForge)** | `fabric/.../platform/*.java`, `neoforge/.../platform/*.java` | Fabric event callbacks (`BlockEvents.USE_WITHOUT_ITEM` vs `UseBlockCallback`, `FabricClientCommandSource.permissions()`); NeoForge event classes (`BreakBlockEvent` vs `BlockEvent.BreakEvent`) and payload registration (`NetworkRegistry.CLIENTBOUND_HANDLERS`). |
| **6** | **Java Toolchain, Architecture Tests & CI** | `gradle.properties`, `CommonArchitectureAndSpiTest.java`, `.github/workflows/build.yml` | New `java_version` / `mixin_compat_level` (if Mojang bumps Java), new client-only packages (e.g., `com.mojang.renderpearl.`) in `CommonArchitectureAndSpiTest`, and JDK provisioning in `.github/workflows/build.yml`. |

### Stage 4: Step-by-Step Execution (Promoting `vcs_version` to `<NEW_VER>`)

1. **Close the previous top node's version range** in `versions/<PREV_TOP>/gradle.properties` so its JAR does not claim compatibility with `<NEW_VER>`:

   ```properties
   neoforge_mc_range=[26.2,26.3)
   ```

2. **Create the new version descriptor** at `versions/<NEW_VER>/gradle.properties` with all 9 required keys:

   ```properties
   # Minecraft 26.3 endpoint (Java 25, unobfuscated Mojmap)
   minecraft_version=26.3
   java_version=25
   mixin_compat_level=JAVA_25
   neoform_version=26.3-1
   neoforge_version=26.3.0.101
   fabric_api_version=0.165.2+26.3
   fabric_mc_range=~26.3
   neoforge_mc_range=[26.3,26.4)
   neoforge_loader_range=[26.3,)
   ```

3. **Promote `vcs_version` and switch Stonecutter BEFORE editing `//? if` blocks**:
   - Update `vcs_version=<NEW_VER>` in root `gradle.properties`.
   - Run `./gradlew "Reset active project" --no-daemon` so `stonecutter.gradle` updates `stonecutter.active '<NEW_VER>'` and Stonecutter rewrites existing comments for `<NEW_VER>`.

4. **Isolate any `<NEW_VER>` API changes using `//? if >=<NEW_VER>`**:
   - Because `<NEW_VER>` is active, write the `<NEW_VER>` code uncommented inside `//? if >=<NEW_VER> {` and wrap the previous behavior inside `//?} else { /*...*///?}`. All 10 older nodes automatically retain the `else` branch without regressions.

5. **Run the full matrix build & test suite**:

   ```bash
   ./gradlew chiseledBuild --no-daemon
   ```

6. **Verify artifacts** in root `build/libs/`:
   - `afkraft_companion-fabric-<mod_version>+<mc_version>.jar`
   - `afkraft_companion-neoforge-<mod_version>+<mc_version>.jar`

---

## Running & Debugging in IntelliJ IDEA

Run configurations are generated automatically per loader and Minecraft version node on **Gradle Sync** (`Reload All Gradle Projects`):

| Loader | IntelliJ Run Configurations (Run / Debug Toolbar) | CLI Equivalent |
|--------|---------------------------------------------------|----------------|
| **Fabric (`<ver>`)** | `Fabric Client (<ver>)` / `Fabric Server (<ver>)` | `./gradlew :fabric:<ver>:runClient` |
| **NeoForge (`<ver>`)** | `NeoForge Client (<ver>)` / `NeoForge Server (<ver>)` | `./gradlew :neoforge:<ver>:runClient` |

- Select any configuration from IntelliJ's top run dropdown and click **Debug** to attach the debugger with breakpoints active across `:domain`, `:common`, `:fabric`, and `:neoforge`.
- Make sure both **Java 21** (for `1.21.x`) and **Java 25** (for `26.x`) toolchains are installed so Gradle can provision the right JVM per node.

---

## Architecture & Module Boundaries

| Module | Java Target | Responsibilities & Constraints |
|--------|-------------|--------------------------------|
| `:domain` | Java 21 (`--release 21`) | Pure business models, container tracking indices, sphere clustering, QR matrix generation, and HTTP/WebSocket auth & snapshot conflation. **Zero `net.minecraft.*` or modloader imports** — never branches per MC version. |
| `:common` | Node `java_version` | Shared Minecraft logic, Mixins, `ServiceLoader` SPIs (`NetworkBridge`, `ServerLifecycleHooks`, `BlockInteractionHooks`, `ClientCommandBridge`, `ScreenWidgetBridge`), and the 5 version-hotspot adapters. Compiled against NeoForm (`net.neoforged.moddev`) with **zero Fabric or NeoForge imports**. |
| `:fabric` | Node `java_version` | `fabric.mod.json`, Fabric entrypoints, and Fabric SPI implementations. Applies `net.fabricmc.fabric-loom-remap` for obfuscated versions (`<26.0`) and `net.fabricmc.fabric-loom` for unobfuscated releases (`>=26.0`). |
| `:neoforge` | Node `java_version` | `META-INF/neoforge.mods.toml`, `@Mod("afkraft_companion")` entrypoints, and NeoForge SPI implementations via `net.neoforged.moddev`. |

---

## Version Hotspot Reference

> For the complete decision matrix on when to extend an existing adapter, create a new `platform.mc` adapter, or add a `platform.spi` loader interface, see [ADAPTERS.md](./ADAPTERS.md).

All Stonecutter directives use exact semantic boundaries verified against Mojmap, Fabric API, and NeoForge across `1.21.1`–`26.3`:

| Seam / File | Exact Version Boundaries | Notes |
|-------------|--------------------------|-------|
| `stonecutter.gradle` (`replacements.string`) | `<1.21.11` vs `>=1.21.11` | Maps `Identifier` <-> `ResourceLocation` automatically. |
| `SavedDataAdapter` & `AFKContainerConfigData` | `<1.21.5` / `1.21.5`–`1.21.11` / `>=26.1` | `SavedData.Factory` (`<1.21.5`) vs `SavedDataType(String, ...)` (`1.21.5`–`1.21.11`) vs `SavedDataType(Identifier, ...)` (`>=26.1`). Preserves legacy `afkraft-companion` read-fallback. |
| `NbtCompatAdapter` | `<1.21.5` vs `>=1.21.5` | `CompoundTag` primitive getters returning raw values + `contains` (`<1.21.5`) vs `Optional<T>` (`>=1.21.5`). |
| `InventoryEquipmentAdapter` & `PlayerInventoryMixin` | `<1.21.4` / `1.21.4` / `>=1.21.5` / `>=26.1` | `pickSlot` + `setPickedItem` (`<1.21.4`), `pickSlot` + `addAndPickItem` & `canItemQuickReplace` (`1.21.4`), `getSelectedSlot`/`setSelectedSlot`/`setSelectedItem` (`>=1.21.5`), `getPlainTextName()` (`>=26.1`). |
| `WorldAndEntityAdapter` | `>=1.21.2` / `>=1.21.4` / `>=1.21.5` / `>=1.21.9` / `>=26.1` | `EquipmentSlot.getId()` & server `InteractionResult.SUCCESS` (`>=1.21.2`), `ServerPlayer.drop(boolean)` void (`>=1.21.4`), `ClickEvent`/`HoverEvent` records & `Entity.snapTo` (`>=1.21.5`), `InteractionHand.asEquipmentSlot()`, `GameProfile` record (`id()`, `name()`), `RespawnData`, `player.level()` (`>=1.21.9`). |
| `PlayerSnapshotMixin` & `HotbarSelectionPacketMixin` | `<1.21.5` / `<26.3` / `>=26.3` | `handleSetCarriedItem` hotbar sync (`<1.21.5`); `ServerPlayer.swing(InteractionHand)` (`<26.3`) vs `ServerPlayer.swingAndResetAttackStrength` + `ServerGamePacketListenerImpl.handlePunch(ServerboundPunchPacket)` (`>=26.3`). |
| `ClientUiAdapter`, `QRModalScreen`, `AuthorizeServerRequestScreen`, `ClientEvents` | `1.21.1` / `>=1.21.2` / `>=1.21.5` / `>=1.21.6` / `>=1.21.9` / `>=1.21.11` / `>=26.1` / `>=26.2` | `GuiGraphics.blit` (`Identifier` in `1.21.1`, `RenderType::guiTextured` in `1.21.2`–`1.21.5`, `RenderPipelines.GUI_TEXTURED` in `>=1.21.6`), `NativeImage.getPixel`/`setPixel` (`>=1.21.5`), `KeyEvent` (`>=1.21.9`), `PermissionSet` & 3-arg `CycleButton.booleanBuilder` (`>=1.21.11`), `GuiGraphicsExtractor` & `addClientSystemMessage` (`>=26.1`), `showDebugChat` (`>=26.2`). |
| `BlockEntityMixin` & `ContainerBlockRemoveMixin` | `<1.21.5` vs `>=1.21.5` | `BlockBehaviour.BlockStateBase.onRemove` `@At("HEAD")` before `Containers.dropContentsOnDestroy` (`<1.21.5`) vs `BlockEntity.preRemoveSideEffects` (`>=1.21.5`). |
| `FabricClientCommandBridge` | `<1.21.6` / `1.21.6`–`1.21.10` / `>=1.21.11` | `source.hasPermission(2)` (`<1.21.6`), `source.getPlayer().hasPermissions(2)` (`1.21.6`–`1.21.10`), `source.permissions().hasPermission(...)` (`>=1.21.11`). |
| `FabricBlockInteractionHooks` & `NeoForgeBlockInteractionHooks` | `<26.2` vs `>=26.2` | `UseBlockCallback.EVENT` vs `BlockEvents.USE_WITHOUT_ITEM` on Fabric; `BlockEvent.BreakEvent` vs `BreakBlockEvent` on NeoForge (post-break callback deferred via `TickTask`). |

---

## Porting & Release Checklist

- [ ] Previous top version's `neoforge_mc_range` is closed (`[<prev>,<new>)`) if `<new>` is a new top node.
- [ ] `versions/<mc_version>/gradle.properties` defines all 9 required keys.
- [ ] If the new version requires a new Java LTS/feature release, add that JDK version to `.github/workflows/build.yml` (`actions/setup-java`) and `CommonArchitectureAndSpiTest`.
- [ ] `./gradlew "Reset active project"` was run so `stonecutter.gradle` points to `vcs_version` (`26.3`) and `common/`, `fabric/`, `neoforge/` hold canonical active sources.
- [ ] `./gradlew chiseledTest` passes all `:domain`, `:common:<ver>`, and `:neoforge:<ver>` JUnit 5 suites across all 10 version nodes.
- [ ] `./gradlew chiseledBuild` succeeds and outputs both Fabric and NeoForge jars for every configured version into `build/libs/`.
