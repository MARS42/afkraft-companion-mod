# Multi-Version & Multi-Loader Maintainer Guide

`afkraft-companion-mod` builds **Fabric** and **NeoForge** jars across multiple Minecraft versions from a single codebase using **Stonecutter** (`0.9.8`), a version-agnostic pure-Java `:domain` module, and isolated Minecraft version adapters in `:common`.

---

## Quick Path: Adding a New Minecraft Version

`settings.gradle` and `stonecutter.gradle` discover every `versions/<mc_version>/gradle.properties` directory automatically — you do not need to register new subprojects manually.

1. **Create the version descriptor** at `versions/<mc_version>/gradle.properties` (copy the closest existing endpoint, e.g. `versions/1.21.1/gradle.properties` or `versions/26.2/gradle.properties`) and fill all 9 required keys:

   ```properties
   minecraft_version=1.21.4
   java_version=21
   mixin_compat_level=JAVA_21
   neoform_version=1.21.4-20241203.161809
   neoforge_version=21.4.123
   fabric_api_version=0.119.2+1.21.4
   fabric_mc_range=~1.21.4
   neoforge_mc_range=[1.21.4,1.21.5)
   neoforge_loader_range=[21.4,)
   ```

2. **Switch Stonecutter's active version** while adapting version-specific APIs so IntelliJ IDEA highlights the target version directly:

   ```bash
   ./gradlew "Set active project to <mc_version>"
   ```

3. **Adapt any Minecraft or loader API changes** inside the version-hotspot adapters (see table below) using Stonecutter conditionals (`//? if >=... {` ... `//?} else {`).

4. **Reset the active VCS version (`26.2`) and run the full matrix build**:

   ```bash
   ./gradlew "Reset active project"
   ./gradlew chiseledBuild
   ```

5. **Inspect collected release artifacts** in root `build/libs/`:
   - `afkraft_companion-fabric-<mod_version>+<mc_version>.jar`
   - `afkraft_companion-neoforge-<mod_version>+<mc_version>.jar`

---

## Running & Debugging in IntelliJ IDEA

Legacy single-module `.idea/runConfigurations/Minecraft_*.xml` files have been removed. Run configurations are now generated automatically per loader and Minecraft version on **Gradle Sync** (`Reload All Gradle Projects`):

| Loader | IntelliJ Run Configurations (Run / Debug Toolbar) | CLI Equivalent |
|--------|---------------------------------------------------|----------------|
| **Fabric (`26.2`)** | `Fabric Client (26.2)` / `Fabric Server (26.2)` | `./gradlew :fabric:26.2:runClient` |
| **NeoForge (`26.2`)** | `NeoForge Client (26.2)` / `NeoForge Server (26.2)` | `./gradlew :neoforge:26.2:runClient` |
| **Fabric (`1.21.1`)** | `Fabric Client (1.21.1)` / `Fabric Server (1.21.1)` | `./gradlew :fabric:1.21.1:runClient` |
| **NeoForge (`1.21.1`)** | `NeoForge Client (1.21.1)` / `NeoForge Server (1.21.1)` | `./gradlew :neoforge:1.21.1:runClient` |

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

When onboarding an intermediate version between `1.21.1` and `26.2` (or a future `26.x+` release), check the exact boundary where Mojang or the loaders changed an API instead of duplicating business logic:

| Seam / File | Version Boundary | Notes |
|-------------|------------------|-------|
| `stonecutter.gradle` (`replacements.string`) | `<1.21.11` vs `>=1.21.11` | Maps `Identifier` <-> `ResourceLocation` automatically. |
| `SavedDataAdapter` & `AFKContainerConfigData` | `1.21.5+` / `26.2` | `SavedData.Factory` + manual `load`/`save` vs `SavedDataType` Codec constructor. Preserves read-fallback from legacy `afkraft-companion:afk_container_config_data`. |
| `NbtCompatAdapter` | `1.21.5+` | `CompoundTag.getString` returning raw `String` vs `Optional<String>`. |
| `InventoryEquipmentAdapter` | `1.21.5+` | `inventory.selected` field vs `getSelectedSlot()`, `DataComponents.CUSTOM_NAME` vs `getPlainTextName()`. |
| `WorldAndEntityAdapter` | `1.21.2`–`26.2` | `player.serverLevel()` vs `player.level()`, `GameProfile` getters vs record accessors (`id()`, `name()`), `ClickEvent` / `HoverEvent` constructors vs sealed records, `minBuildHeight` vs `getMinY`, `LevelData.RespawnData`. |
| `ClientUiAdapter`, `QRModalScreen`, `AuthorizeServerRequestScreen`, `ClientEvents` | `1.21.2`–`26.2` | `GuiGraphics` (`render`) vs `GuiGraphicsExtractor` (`extractRenderState`), `NativeImage` `getPixelRGBA`/`setPixelRGBA` vs `getPixel`/`setPixel`, `DynamicTexture` label supplier, `KeyEvent`, `CycleButton.builder`, `PermissionSet` vs `hasPermission(2)`. |
| `BlockEntityMixin` & `ContainerBlockRemoveMixin` | `1.21.5+` / `26.2` | `BlockEntity.preRemoveSideEffects` (`>=26.2` / `1.21.5+`) vs `BlockBehaviour.BlockStateBase.onRemove` `@At("HEAD")` before `Containers.dropContentsOnDestroy` (`1.21.1`–`1.21.4`). |
| `FabricBlockInteractionHooks` & `NeoForgeBlockInteractionHooks` | Loader-specific | `UseBlockCallback` vs `BlockEvents.USE_WITHOUT_ITEM` on Fabric; `BlockEvent.BreakEvent` vs `BreakBlockEvent` on NeoForge (always keep post-break callback deferred via `TickTask`). |

---

## Porting & Release Checklist

- [ ] `versions/<mc_version>/gradle.properties` defines all 9 required keys.
- [ ] If the new version requires a new Java LTS/feature release, add that JDK version to `.github/workflows/build.yml` (`actions/setup-java`).
- [ ] `./gradlew "Reset active project"` was run so `stonecutter.gradle` points to `vcs_version` (`26.2`) and `common/`, `fabric/`, `neoforge/` hold canonical active sources.
- [ ] `./gradlew chiseledTest` passes all `:domain`, `:common:<ver>`, and `:neoforge:<ver>` JUnit 5 suites.
- [ ] `./gradlew chiseledBuild` succeeds and outputs both Fabric and NeoForge jars for every configured version into `build/libs/`.
