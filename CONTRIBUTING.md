# Contributing to AFKraft Companion

Thank you for your interest in improving **AFKraft Companion**. Bug reports, localization improvements, version ports, and pull requests are welcome.

## Quick Path

1. Install **JDK 21** (for Minecraft `1.21.1`–`1.21.11`) and **JDK 25** (for Minecraft `26.1.2`–`26.3`).
2. Clone the repository and make your changes against the active Stonecutter VCS version (`26.3`).
3. Verify purity, localization parity, and multi-version compilation before opening a pull request:

```bash
./gradlew "Reset active project" --no-daemon
./gradlew :domain:check --no-daemon
./gradlew :common:26.3:check --no-daemon
./gradlew chiseledTest --no-daemon
```

## Architecture & Quality Rules

| Area | Rule |
|------|------|
| `:domain` purity | Zero `net.minecraft.*`, `net.fabricmc.*`, or `net.neoforged.*` imports (`DomainArchitecturePurityTest`). |
| `:common` isolation | Zero Fabric or NeoForge loader imports; use `ServiceLoader` SPIs (`CommonArchitectureAndSpiTest`). |
| Translations | Keep `en_us.json`, `es_es.json`, and `es_mx.json` in exact key and placeholder parity (`LocalizationParityTest`). |
| Stonecutter hygiene | Always run `./gradlew "Reset active project"` before committing so `common/`, `fabric/`, and `neoforge/` remain aligned on `vcs_version`. |
| Multi-version porting | Follow [VERSIONING.md](./VERSIONING.md) and [ADAPTERS.md](./ADAPTERS.md) before adding `//? if` directives. |

## Contributor License Agreement (Inbound License Grant)

AFKraft Companion is distributed under the [PolyForm Shield License 1.0.0](./LICENSE).

By submitting a Pull Request, patch, or other contribution to this repository, you agree to license your contribution to the project maintainer (**IKangreburguito**) under the **Apache License, Version 2.0** (superseding the default inbound=outbound terms of GitHub's Terms of Service Section D.6).

This grant ensures that:
- You retain copyright ownership of your individual contribution.
- The maintainer receives a perpetual, worldwide, non-exclusive, royalty-free, irrevocable copyright and patent license to use, modify, sublicense, and distribute your contribution as part of AFKraft Companion on GitHub, Modrinth, CurseForge, or any future release or license of the project.
- You confirm that you have the legal right to submit the contribution and that it does not infringe third-party rights.

If you do not agree to these terms, please do not submit a pull request.
