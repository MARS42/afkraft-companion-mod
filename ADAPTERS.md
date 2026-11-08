# Adapter & SPI Architecture Guide

`afkraft-companion-mod` isolates **Minecraft version changes** (`platform.mc`) from **mod-loader differences** (`platform.spi`) so that `:domain` stays 100% pure Java and `:common/core` remains clean of version-specific `//? if` directives wherever possible.

---

## Quick Decision Matrix: Where Does a Change Belong?

| What changed or is being added? | Action to take | Target package / module |
|---------------------------------|----------------|-------------------------|
| Pure business logic, JSON schema, auth, WebSocket/HTTP, sphere math, or container tracking | Edit `:domain` directly (no adapters or Stonecutter directives needed) | `domain/src/main/java/com/mars4two/ikang/domain/...` |
| Minecraft changed a method signature, NBT API, inventory/component API, world/entity API, or UI rendering in an area we already use | **Extend one of the 5 existing Minecraft version adapters** using `//? if` | `common/src/{main,client}/java/com/mars4two/ikang/{,client/}platform/mc/...` |
| The mod adopts a **brand-new Minecraft subsystem** (e.g., custom particles, sounds, worldgen, recipes) whose vanilla API differs across MC versions | **Create a new `*Adapter` class** in `platform.mc` (or `client.platform.mc`) | `common/src/{main,client}/java/com/mars4two/ikang/{,client/}platform/mc/...` |
| A feature requires **mod-loader-specific hooks** (Fabric vs. NeoForge events, networking registration, key mappings, screen events) | **Extend an existing SPI or add a new `ServiceLoader` SPI** in `platform.spi` and implement it in `:fabric` and `:neoforge` | `common/.../platform/spi/`, `fabric/.../platform/`, `neoforge/.../platform/` |
| Two adjacent Minecraft patches are binary-compatible for everything the mod touches (e.g., `1.21` + `1.21.1`, or `1.21.2` + `1.21.3`) | **Do not add a new Stonecutter node or adapter branch** — widen `fabric_mc_range` and `neoforge_mc_range` in `versions/<ver>/gradle.properties` | `versions/<ver>/gradle.properties` |

---

## 1. Minecraft Versioning Mental Model

1. **Mojang does not follow SemVer internally**: Minor "Game Drop" updates (`1.21.1` -> `1.21.2` -> `1.21.4` -> `1.21.5`) frequently change vanilla method signatures, `CompoundTag` return types, `DataComponents`, and `GuiGraphics` rendering APIs.
2. **Compatibility Clusters ("Version Islands")**:
   - Not every patch breaks binary compatibility. Clusters such as `1.21`–`1.21.1` or `1.21.2`–`1.21.3` usually share identical method descriptors for the classes this mod touches.
   - Only create a new `versions/<mc_version>/gradle.properties` node when a Minecraft release breaks binary compatibility or loader metadata for our code.
3. **Obfuscated (`<=1.21.11`, Java 21) vs. Unobfuscated (`26.1+`, Java 25)**:
   - Versions up to `1.21.11` ship obfuscated official JARs; `:fabric` uses `net.fabricmc.fabric-loom-remap` to remap Mojmap bytecode to Intermediary at build time.
   - Versions `26.1+` ship unobfuscated Mojmap JARs on Java 25; `:fabric` uses `net.fabricmc.fabric-loom` without remapping.
4. **Range-Based Stonecutter Conditionals**:
   - Because our Stonecutter blocks use semantic ranges (`//? if >=1.21.2`, `//? if >=1.21.5`, `//? if >=26.1`) rather than hardcoded version equalities, adding an intermediate version in `settings.gradle` automatically selects the right code branch when that boundary was already mapped between `1.21.1` and `26.2`.

---

## 2. Catalog of Existing Minecraft Version Adapters (`platform.mc`)

Always check if a change fits one of these five adapters before creating a new class:

| Adapter | Side | Responsibility | Key Methods & Version Boundaries Handled |
|---------|------|----------------|------------------------------------------|
| `SavedDataAdapter` | Server (`main`) | World persistence (`DimensionDataStorage` / `SavedDataStorage`) and legacy `afkraft-companion` -> `afkraft_companion` file migration | `getOrCreateContainerConfig(ServerLevel, ...)` (`SavedDataType` Codec in `>=1.21.5` vs. `SavedData.Factory` + `HolderLookup.Provider` in `<1.21.5`). |
| `NbtCompatAdapter` | Common (`main`) | `CompoundTag` primitive reads across NBT API overhauls | `getStringOrDefault(CompoundTag, String, String)` (`Optional<String>` in `>=1.21.5` vs. raw `String` + `contains(..., 8)` in `<1.21.5`). |
| `InventoryEquipmentAdapter` | Common (`main`) | Player inventory slots, hotbar selection, equipment iteration, and item display names | `getSelectedHotbarSlot`, `setSelectedHotbarSlot`, `getItemCustomName`, `getItemPlainName` (`getSelectedSlot()` & `DataComponents.CUSTOM_NAME` differences across `1.21.1`–`26.2`). |
| `WorldAndEntityAdapter` | Common (`main`) | Level/dimension keys, `ServerPlayer` accessors, `EquipmentSlot` protocol IDs, block placement result checks, chat `ClickEvent`/`HoverEvent`, and server `TickTask` scheduling | `serverLevel`, `dimensionId`, `getMinBuildHeight`, `getEquipmentSlotProtocolId`, `isSuccessfulBlockPlacement` (`CONSUME` on `1.21.1` server vs. `SUCCESS` on `26.2`), `scheduleNextServerTask`. |
| `ClientUiAdapter` | Client (`client`) | `Screen` widgets, `GuiGraphics` / `GuiGraphicsExtractor` text, lines, outlines, `DynamicTexture` / `NativeImage` QR pixel writes, and client permission checks | `drawCenteredText`, `drawText`, `drawWordWrap`, `drawBoxOutline`, `setQrPixel`, `uploadQrTexture`, `createBooleanCycleButton`, `hasOpPermissionLevel`. |

### Example: Extending an Existing Adapter

When a Minecraft update changes a vanilla method signature (for example, how a player's ping/latency is read on `ServerPlayer`), add a static helper to `WorldAndEntityAdapter` instead of scattering `//? if` across `core/`:

```java
// In WorldAndEntityAdapter.java
public static int getPlayerLatencyMs(ServerPlayer player) {
    //? if >=1.21.2 {
    return player.connection.latency();
    //?} else {
    /*return player.latency;
    *///?}
}
```

---

## 3. Catalog of Loader SPIs (`platform.spi`)

Loader SPIs decouple `:common` from **Fabric** and **NeoForge**. They are loaded once via `Services.load(...)` (`java.util.ServiceLoader`) and never contain Minecraft version logic directly unless a loader's own event API changed across versions:

| SPI Interface | Side | Fabric Implementation | NeoForge Implementation | Purpose |
|---------------|------|-----------------------|-------------------------|---------|
| `NetworkBridge` | Common + Client | `FabricNetworkBridge` | `NeoForgeNetworkBridge` | Payload codec registration (`S2C`/`C2S`), server-to-client packet sending, and client-to-server packet sending. |
| `ServerLifecycleHooks` | Common (`main`) | `FabricServerLifecycleHooks` | `NeoForgeServerLifecycleHooks` | `SERVER_STARTED`, `SERVER_STOPPING`, `END_SERVER_TICK`, and player join/disconnect hooks. |
| `BlockInteractionHooks` | Common (`main`) | `FabricBlockInteractionHooks` | `NeoForgeBlockInteractionHooks` | Right-click block inspection (`InteractionResult`) and deferred post-block-break cleanup. |
| `ClientCommandBridge` | Client (`client`) | `FabricClientCommandBridge` | `NeoForgeClientCommandBridge` | Client-side `/afkraft_companion` subcommands (`connect`, `status`, `disconnect`). |
| `ScreenWidgetBridge` | Client (`client`) | `FabricScreenWidgetBridge` | `NeoForgeScreenWidgetBridge` | Injecting the AFK toggle button into container screens after `Screen.init`. |

---

## 4. Checklist: Adding a New Adapter or SPI

- [ ] **Verify `:domain` purity first**: Can this logic live in `:domain` using pure Java types? If yes, put it in `:domain` and convert Minecraft types at the `:common` boundary.
- [ ] **Check the 5 existing `platform.mc` adapters**: Does the change belong to persistence (`SavedDataAdapter`), NBT (`NbtCompatAdapter`), inventory/items (`InventoryEquipmentAdapter`), world/entities/server (`WorldAndEntityAdapter`), or client UI (`ClientUiAdapter`)?
- [ ] **Use semantic version ranges in Stonecutter comments**: Always write `//? if >=1.21.5 {` ... `//?} else {` rather than exact version equality so intermediate versions inherit the right branch automatically.
- [ ] **Keep canonical sources on `vcs_version` (`26.2`)**: Run `./gradlew "Reset active project"` before committing so active sources in `common/`, `fabric/`, and `neoforge/` remain on `26.2`.
- [ ] **Verify all targets**: Run `./gradlew chiseledBuild` (see [VERSIONING.md](./VERSIONING.md)) to confirm both `1.21.1` and `26.2` compile and pass all `:domain`, `:common`, and `:neoforge` JUnit 5 tests.
