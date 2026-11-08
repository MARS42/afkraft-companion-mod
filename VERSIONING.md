# Multi-Version & Multi-Loader Maintainer Guide

`afkraft-companion-mod` builds **Fabric** and **NeoForge** jars across **15 Minecraft releases (`1.21.1` through `26.2`)** from a single codebase using **Stonecutter** (`0.9.8`), a version-agnostic pure-Java `:domain` module, and isolated Minecraft version adapters in `:common`.

---

## Supported Version Nodes Matrix

Instead of duplicating a build node for every minor patch when Mojang's bytecode is binary-compatible for all symbols the mod touches, the build uses **9 Stonecutter compatibility-cluster nodes** (producing 18 release JARs):

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
| `26.2` | Java 25 | `~26.2` (`26.2`) | `[26.2,26.3)` (`26.2`) | Active VCS version (`vcs_version`); `BlockEvents.USE_WITHOUT_ITEM`, `BreakBlockEvent` |

---

## Quick Path: Adding a New Minecraft Version

`settings.gradle` and `stonecutter.gradle` discover every `versions/<mc_version>/gradle.properties` directory automatically — you do not need to register new subprojects manually.

1. **Check whether a new node is needed**: If the new patch is binary-compatible with an adjacent node (e.g. a hotfix patch), widen `fabric_mc_range` / `neoforge_mc_range` in the existing `versions/<mc_version>/gradle.properties` instead of creating a new node.

2. **Create the version descriptor** at `versions/<mc_version>/gradle.properties` (copy the closest existing endpoint) and fill all 9 required keys:

   ```properties
   minecraft_version=1.21.4
   java_version=21
   mixin_compat_level=JAVA_21
   neoform_version=1.21.4-20241203.161809
   neoforge_version=21.4.161
   fabric_api_version=0.119.4+1.21.4
   fabric_mc_range=~1.21.4
   neoforge_mc_range=[1.21.4,1.21.5)
   neoforge_loader_range=[21.4,)
   ```

3. **Switch Stonecutter's active version** while adapting version-specific APIs so IntelliJ IDEA highlights the target version directly:

   ```bash
   ./gradlew "Set active project to <mc_version>"
   ```

4. **Adapt any Minecraft or loader API changes** inside the version-hotspot adapters (see table below) using Stonecutter conditionals (`//? if >=... {` ... `//?} else {`).

5. **Reset the active VCS version (`26.2`) and run the full matrix build**:

   ```bash
   ./gradlew "Reset active project"
   ./gradlew chiseledBuild
   ```

6. **Inspect collected release artifacts** in root `build/libs/`:
   - `afkraft_companion-fabric-<mod_version>+<mc_version>.jar`
   - `afkraft_companion-neoforge-<mod_version>+<mc_version>.jar`

---

## Running & Debugging in IntelliJ IDEA

Legacy single-module `.idea/runConfigurations/Minecraft_*.xml` files have been removed. Run configurations are generated automatically per loader and Minecraft version node on **Gradle Sync** (`Reload All Gradle Projects`):

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

All Stonecutter directives use exact semantic boundaries verified against Mojmap, Fabric API, and NeoForge across `1.21.1`–`26.2`:

| Seam / File | Exact Version Boundaries | Notes |
|-------------|--------------------------|-------|
| `stonecutter.gradle` (`replacements.string`) | `<1.21.11` vs `>=1.21.11` | Maps `Identifier` <-> `ResourceLocation` automatically. |
| `SavedDataAdapter` & `AFKContainerConfigData` | `<1.21.5` / `1.21.5`–`1.21.11` / `>=26.1` | `SavedData.Factory` (`<1.21.5`) vs `SavedDataType(String, ...)` (`1.21.5`–`1.21.11`) vs `SavedDataType(Identifier, ...)` (`>=26.1`). Preserves legacy `afkraft-companion` read-fallback. |
| `NbtCompatAdapter` | `<1.21.5` vs `>=1.21.5` | `CompoundTag` primitive getters returning raw values + `contains` (`<1.21.5`) vs `Optional<T>` (`>=1.21.5`). |
| `InventoryEquipmentAdapter` & `PlayerInventoryMixin` | `<1.21.4` / `1.21.4` / `>=1.21.5` / `>=26.1` | `pickSlot` + `setPickedItem` (`<1.21.4`), `pickSlot` + `addAndPickItem` & `canItemQuickReplace` (`1.21.4`), `getSelectedSlot`/`setSelectedSlot`/`setSelectedItem` (`>=1.21.5`), `getPlainTextName()` (`>=26.1`). |
| `WorldAndEntityAdapter` | `>=1.21.2` / `>=1.21.4` / `>=1.21.5` / `>=1.21.9` / `>=26.1` | `EquipmentSlot.getId()` & server `InteractionResult.SUCCESS` (`>=1.21.2`), `ServerPlayer.drop(boolean)` void (`>=1.21.4`), `ClickEvent`/`HoverEvent` records & `Entity.snapTo` (`>=1.21.5`), `InteractionHand.asEquipmentSlot()`, `GameProfile` record (`id()`, `name()`), `RespawnData`, `player.level()` (`>=1.21.9`). |
| `ClientUiAdapter`, `QRModalScreen`, `AuthorizeServerRequestScreen`, `ClientEvents` | `1.21.1` / `>=1.21.2` / `>=1.21.5` / `>=1.21.6` / `>=1.21.9` / `>=1.21.11` / `>=26.1` / `>=26.2` | `GuiGraphics.blit` (`Identifier` in `1.21.1`, `RenderType::guiTextured` in `1.21.2`–`1.21.5`, `RenderPipelines.GUI_TEXTURED` in `>=1.21.6`), `NativeImage.getPixel`/`setPixel` (`>=1.21.5`), `KeyEvent` (`>=1.21.9`), `PermissionSet` & 3-arg `CycleButton.booleanBuilder` (`>=1.21.11`), `GuiGraphicsExtractor` & `addClientSystemMessage` (`>=26.1`), `showDebugChat` (`>=26.2`). |
| `BlockEntityMixin` & `ContainerBlockRemoveMixin` | `<1.21.5` vs `>=1.21.5` | `BlockBehaviour.BlockStateBase.onRemove` `@At("HEAD")` before `Containers.dropContentsOnDestroy` (`<1.21.5`) vs `BlockEntity.preRemoveSideEffects` (`>=1.21.5`). |
| `FabricClientCommandBridge` | `<1.21.6` / `1.21.6`–`1.21.10` / `>=1.21.11` | `source.hasPermission(2)` (`<1.21.6`), `source.getPlayer().hasPermissions(2)` (`1.21.6`–`1.21.10`), `source.permissions().hasPermission(...)` (`>=1.21.11`). |
| `FabricBlockInteractionHooks` & `NeoForgeBlockInteractionHooks` | `<26.2` vs `>=26.2` | `UseBlockCallback.EVENT` vs `BlockEvents.USE_WITHOUT_ITEM` on Fabric; `BlockEvent.BreakEvent` vs `BreakBlockEvent` on NeoForge (post-break callback deferred via `TickTask`). |

---

## Porting & Release Checklist

- [ ] `versions/<mc_version>/gradle.properties` defines all 9 required keys.
- [ ] If the new version requires a new Java LTS/feature release, add that JDK version to `.github/workflows/build.yml` (`actions/setup-java`).
- [ ] `./gradlew "Reset active project"` was run so `stonecutter.gradle` points to `vcs_version` (`26.2`) and `common/`, `fabric/`, `neoforge/` hold canonical active sources.
- [ ] `./gradlew chiseledTest` passes all `:domain`, `:common:<ver>`, and `:neoforge:<ver>` JUnit 5 suites across all 9 version nodes.
- [ ] `./gradlew chiseledBuild` succeeds and outputs both Fabric and NeoForge jars for every configured version into `build/libs/`.
