# Privacy & Network Disclosure

**AFKraft Companion** is an opt-in companion mod that streams live Minecraft gameplay data to the AFKraft web platform while you are AFK or managing farms. This document explains every outbound network connection the mod makes, what data is transmitted, what is persisted, and how you control it.

---

## Summary of Outbound Connections

| Endpoint | When It Runs | Consent Model | Purpose |
|----------|--------------|---------------|---------|
| `https://raw.githubusercontent.com/MARS42/afkraft-companion-mod/main/update.json` | Once on client startup and dedicated server startup | Automatic (read-only check) | Checks whether a newer release of the mod is available for your Minecraft version. |
| Official Mojang Session Server (via Minecraft client) | Only when linking an account or authorizing a server | Strictly Opt-In | Proves Minecraft account ownership (`joinServer` / `hasJoined`) without ever handling Microsoft passwords. |
| `https://api-mvp.afkraft.com/core` and `wss://api-mvp.afkraft.com/core/mod-worker/snapshots/ws` | Only after explicit linking (`/afkraft connect`) and world/server activation (`/afkraft enable` or clicking **Authorize**) | Strictly Opt-In | Authenticates the session, syncs snapshot preferences, and streams live gameplay snapshots to your AFKraft dashboard. |

---

## 1. Automatic Startup Update Check

When Minecraft starts (client) or a dedicated server finishes starting, the mod sends one read-only `HTTPS GET` request to the public `update.json` file hosted on GitHub.

- **Sent data**: Standard HTTP request with `User-Agent: AFKraftCompanion/<mod_version>`.
- **Not sent**: No player UUID, username, world name, server IP, or gameplay data is included.
- **On NeoForge**: NeoForge's built-in `updateJSONURL` mechanism reads the same manifest.

---

## 2. Strictly Opt-In AFKraft Platform Sync

Zero requests are sent to `api-mvp.afkraft.com` unless a player or server administrator explicitly initiates one of the following actions:

1. **Account Linking (`/afkraft connect`)**: Displays a privacy-guarded QR modal (`Show QR` button) to link your Minecraft profile to your AFKraft web account using Mojang session verification.
2. **Server / World Activation (`/afkraft enable`)**: An authorized administrator (or singleplayer world owner) enables AFKraft for the active world/server.
3. **Multiplayer Server Authorization (`AuthorizeServerRequestScreen`)**: On dedicated servers where an admin enabled AFKraft, each linked player is prompted in-game to explicitly click **Authorize** or **Not now** before their data is streamed.

### Technical Environment Headers
Requests to `api-mvp.afkraft.com` include three technical compatibility headers so the backend can handle version-specific snapshot formats:
- `X-AFKraft-Game-Version` (e.g., `26.3`)
- `X-AFKraft-Mod-Version` (e.g., `1.0.0-beta.1`)
- `X-AFKraft-Mod-Loader` (`fabric` or `neoforge`)

---

## 3. Ephemeral vs. Persisted Data

### Ephemeral Session Data (RAM Only)
When enabled and authorized, the mod streams live snapshots over authenticated HTTPS/WebSockets for active display in the AFKraft webapp:
- Player stats (health, hunger, air, XP, game mode, coordinates)
- Active potion/beacon effects
- Held items, hotbar, inventory, and equipped armor (including durability & enchantments)
- Attack cadence and weapon cooldown metrics
- 128-block mob despawn sphere counts and clusters
- Tracked container contents (chests, hoppers, barrels, furnaces, portable shulkers) and shulker load stations

These gameplay snapshots are **ephemeral**: they are held only in server memory for the duration of the active game session and are not permanently archived as historical gameplay logs.

### Persisted Integration Metadata
To keep your account, worlds, and settings wired between sessions, the AFKraft backend persists minimal integration records:
- Player Minecraft UUID and username
- Registered world / server identifier and display name
- Per-world snapshot toggle preferences (which snapshot categories are enabled or disabled)

### Local Files on Your Machine / Server
- **World `SavedData`**: Stores which container positions and portable shulker UUIDs are flagged for AFK tracking inside the Minecraft world folder.
- **Client UI Config**: Stores the custom screen offset for the in-container AFK button locally.
- **Server Session Tokens (`CredentialStore`)**: Held **only in memory** while the server is running and cleared automatically when the server stops.

---

## 4. User Controls & Opt-Out

- **Do nothing**: If you never run `/afkraft connect` or `/afkraft enable`, the mod never transmits gameplay data to AFKraft.
- **Decline multiplayer servers**: Click **Not now** on the server authorization dialog to prevent a dedicated server from streaming your session.
- **Granular snapshot toggles**: Run `/afkraft config` (or press the Snapshot Preferences keybind) to disable any of the 10 snapshot categories for your world.
- **Inspect the code**: The complete mod source code is publicly auditable at <https://github.com/MARS42/afkraft-companion-mod>.

---

## Contact

For privacy questions or data removal requests related to your linked AFKraft account, open an issue at <https://github.com/MARS42/afkraft-companion-mod/issues> or use the support channel inside the AFKraft web application.
