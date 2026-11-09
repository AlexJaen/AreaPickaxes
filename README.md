<h1 align="center">⛏️ AreaPickaxes</h1>
<p align="center"><b>Pickaxes that mine a whole area at once — 2x2, 3x3, 2x3… up to 9x9.</b></p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-1.1.0-blue">
  <img alt="Paper" src="https://img.shields.io/badge/Paper-26.2-success">
  <img alt="Java" src="https://img.shields.io/badge/Java-25-orange">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-lightgrey">
</p>

| | |
|---|---|
| **Version** | 1.1.0 |
| **Platform** | Paper **26.2** (Java 25) |
| **Dependencies** | None (GrimAC optional) |
| **Command** | `/apick give <player> <WIDTHxHEIGHT> [material]` |

## What is it?

AreaPickaxes lets admins hand out special pickaxes that break a rectangle of blocks around the one you hit — perfect for prison, mining-world and tunnel-digging servers. Normal players can't create them; you decide who gets which size, and how (kits, crates, shops, rewards… anything that can run a command).

## Features

- 📐 **Any size from 1x1 to 9x9** (2x2, 2x3, 3x3, 5x1…), stored on the item itself.
- 🧭 **View-aware**: the area is applied on the plane you are facing, so horizontal tunnels give wall-shaped areas and digging up/down gives floor-shaped ones.
- 🛡️ **Protection friendly**: every extra block is broken through `Player#breakBlock`, so a real `BlockBreakEvent` is fired for each one. WorldGuard, CoreProtect, economy/prison plugins, mcMMO, etc. behave as if the player broke each block by hand.
- 📦 **Container-safe**: chests, barrels, furnaces… broken as extra blocks keep their contents.
- 🚫 **Dupe-proof shulker boxes**: shulker boxes are never broken as "extra" blocks (their contents already travel inside the dropped item).
- 🧰 **Any pickaxe material** (`wooden` … `netherite`), default `diamond_pickaxe`.
- 🛑 **GrimAC friendly** *(optional)*: cancels only the *AirLiquidBreak* flags caused by area breaks, nothing else.
- ⚡ **Lightweight**: no config, no database, no scheduler tasks.

## Commands & permissions

| Command | Description | Permission | Default |
|---|---|---|---|
| `/apick give <player> <WIDTHxHEIGHT> [material]` | Give an area pickaxe | `areapickaxes.admin.give` | op |

Examples:

```
/apick give Steve 3x3
/apick give Steve 2x3 netherite_pickaxe
```

Tab-completion is available for all arguments.

## Installation

1. Download the latest `areapickaxes-<version>.jar` from the [Releases](../../releases) page.
2. Drop it into your server's `plugins/` folder.
3. Restart the server.

**Requirements:** Paper 26.2, Java 25.

## Build from source

```bash
git clone https://github.com/AlexJaen/AreaPickaxes.git
cd AreaPickaxes
mvn clean package
```

The jar is written to `target/areapickaxes-<version>.jar`. Requires **JDK 25** and Maven.

## Changelog

| Version | Notes |
|---|---|
| **1.1.0** | Ported to Paper 26.2 / Java 25. Fixed an item-duplication exploit with shulker boxes broken as extra blocks. |
| 1.0.2 | Containers broken as extra blocks no longer lose their contents. |
| 1.0.0 | First release. |

## Security note

If you fork this plugin, never re-drop the contents of a block whose vanilla drop already contains them (shulker boxes). See the comments in `AreaPickaxeListener`.

## License

MIT — see [LICENSE](LICENSE).

---

### 🇪🇸 Resumen

Picos de minería en área (hasta 9x9) que solo se entregan por comando de administrador. Compatible con protecciones y economía (cada bloque dispara su propio `BlockBreakEvent`). Las shulker box no se rompen como bloque extra para evitar duplicación de items. Requiere Paper 26.2 y Java 25.
