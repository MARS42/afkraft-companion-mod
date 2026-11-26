# AFKraft Companion

Lightweight, multi-version Minecraft companion mod for **Fabric** and **NeoForge** (`1.21.1`–`26.3`) that streams opt-in AFK farm, container, player, and mob-sphere telemetry in real time to the **AFKraft** web dashboard.

> [!NOTE]
> **Closed Beta (`1.0.0-beta.1`)**: AFKraft Companion is currently in closed pre-release while the full AFKraft web platform rolls out to early testers. Linking an account via `/afkraft connect` requires closed-beta access on the AFKraft webapp.

---

## Quick Start

1. **Install on both Client and Server** (or Singleplayer):
   - **Fabric**: Requires **Fabric Loader** and **Fabric API**.
   - **NeoForge**: Requires **NeoForge** for your exact Minecraft version.
2. **Link your account** in-game with `/afkraft connect` and scan the privacy-guarded QR code from your AFKraft dashboard.
3. **Enable your world/server** with `/afkraft enable` (singleplayer owner or server admin).
4. **Track containers** by clicking the `AFK` mode toggle button inside chests, hoppers, barrels, furnaces, shulkers, or shulker load stations.
5. **Customize streamed data** at any time via `/afkraft config` or the **Snapshot Preferences** keybind.

---

## Tracked Snapshots (100% Configurable)

| Category | What Is Monitored |
|----------|-------------------|
| **Player Stats** | Health, hunger, oxygen, experience, game mode, and coordinates |
| **Effects** | Active potion and beacon effects with amplifier and remaining duration |
| **Held Items & Hotbar** | Main hand, off-hand, and 9 hotbar slots with durability and enchantments |
| **Inventory & Armor** | Main 27-slot inventory, equipped armor pieces, durability, and armor points |
| **Attack Cadence** | Attack rhythm, cooldown charge, and sweep timing for AFK combat farms |
| **Mob Despawn Sphere** | Mob cap counts, species breakdown, and clusters within the 128-block radius |
| **Containers & Load Stations** | Chests, hoppers, barrels, furnaces, portable shulkers, and shulker loader fill progress |

Localized in-game in **English (`en_us`)**, **Spanish - Spain (`es_es`)**, and **Spanish - Mexico (`es_mx`)**.

---

## Compatibility Matrix

| Minecraft Versions | Java | Fabric (Requires Fabric API) | NeoForge |
|--------------------|------|------------------------------|----------|
| `1.21.1` – `1.21.11` | Java 21 | `1.21.1`–`1.21.11` | `1.21.1`, `1.21.3`–`1.21.5`, `1.21.8`, `1.21.10`, `1.21.11` |
| `26.1` – `26.3` | Java 25 | `26.1`–`26.1.2`, `26.2`, `26.3` | `26.1.2`, `26.2`, `26.3` |

- **Environment**: Required on **both Client and Server** (registers custom bidirectional network payloads). Works automatically in Singleplayer.

---

## Privacy & Network Transparency

- **Strictly opt-in streaming**: Zero gameplay data is transmitted to AFKraft servers until you run `/afkraft connect`, `/afkraft enable`, or approve a server prompt.
- **Ephemeral session snapshots**: Live gameplay snapshots are held only in server RAM during your active session. Only minimal integration metadata (Minecraft UUID, username, world name, and snapshot preferences) is persisted.
- **Startup update check**: Performs one read-only `HTTPS GET` to this repository's `update.json` with zero player or world identifiers.

Read the full disclosure in [**PRIVACY.md**](./PRIVACY.md).

---

## Architecture & Maintainer Documentation

Built from a single multi-version codebase using a pure-Java `:domain` module, `ServiceLoader` loader SPIs, and [Stonecutter](https://stonecutter.kikugie.dev/) (`10` compatibility nodes producing `20` release JARs):

- [**RELEASING.md**](./RELEASING.md) — Versioning contract, `update.json` governance, and controlled Modrinth/CurseForge/GitHub release runbook.
- [**VERSIONING.md**](./VERSIONING.md) — Multi-version compatibility matrix and porting guide for new Minecraft releases.
- [**ADAPTERS.md**](./ADAPTERS.md) — Architecture decision matrix and catalog of Minecraft version adapters and loader SPIs.
- [**CONTRIBUTING.md**](./CONTRIBUTING.md) — Local build prerequisites, verification checks, and inbound contributor license terms.

---

## License & Notices

AFKraft Companion is source-available under the [**PolyForm Shield License 1.0.0**](./LICENSE). See [**NOTICE**](./NOTICE) for required copyright notices, Modrinth/CurseForge modpack redistribution clarity, and third-party notices for bundled **ZXing Core (`Apache-2.0`)**.

*NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.*
