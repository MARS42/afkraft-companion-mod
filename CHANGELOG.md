# Changelog

All notable changes to **AFKraft Companion** are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project uses Semantic Versioning (`MAJOR.MINOR.PATCH` with Maven/SemVer-compatible pre-release tags such as `-beta.1` and `-rc.1`).

## [1.0.0-beta.1]

### Added
- Initial closed-beta pre-release supporting **Fabric** (with Fabric API) and **NeoForge** across **10 Stonecutter compatibility nodes (`1.21.1` through `26.3`)**.
- Opt-in account linking (`/afkraft connect`) with privacy-guarded QR modal and Mojang session server verification.
- Admin world/server activation (`/afkraft enable`) and per-player dedicated server authorization dialog.
- Real-time ephemeral snapshot streaming over HTTPS/WebSockets for 10 configurable categories: player stats, effects, held items, attack cadence, hotbar, inventory, armor/equipment, 128-block mob despawn sphere, tracked containers, and shulker load stations.
- In-game Snapshot Preferences GUI (`/afkraft config` + keybind) and draggable Container Button Position editor.
- Full localization parity in English (`en_us`), Spanish Spain (`es_es`), and Spanish Mexico (`es_mx`).
- Non-blocking update checker backed by `update.json` for Fabric clients, NeoForge Mods screen, and dedicated server consoles.
- Adopted **PolyForm Shield License 1.0.0** with `NOTICE`, `CONTRIBUTING.md`, `PRIVACY.md`, and bundled Apache-2.0 attribution for `com.google.zxing:core:3.5.3`.
