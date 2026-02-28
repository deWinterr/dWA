# Skyblock Profit Tracker — Fabric 1.21.10

A client-side Fabric mod for Minecraft 1.21.10 that tracks ore and gemstone mining profits in Hypixel SkyBlock. A port/combination of the ChatTriggers 1.8.9 modules **BigDiamond** and **BlingBlingAddons** into a modern Fabric mod.

## Features

### Ore Tracking (from BigDiamond)
- Detects `[Sacks] +X items. (Last Ys.)` chat messages
- Parses hover text to identify individual ore types
- Handles enchanted item multipliers (×160) and block multipliers (×160²)
- Tracks: Diamond, Coal, Iron, Gold, Emerald, Lapis, Redstone, Obsidian, Mithril, Titanium, End Stone, Glacite, Umber, Tungsten

### Gemstone Tracking (from BlingBlingAddons)
- Detects `PRISTINE! You found a Flawed X Gemstone x#!` messages
- Tracks all gemstone types: Ruby, Amethyst, Jade, Sapphire, Amber, Topaz, Jasper, Aquamarine, Citrine, Peridot, Onyx
- Counts flawed gemstone procs with accurate per-gem pricing

### Pricing
- **Bazaar Instant Sell** — Real-time prices from Hypixel Bazaar API (default)
- **Bazaar Sell Offer** — Buy order prices for higher estimates
- **NPC Prices** — Static NPC sell values as fallback
- Automatically picks `max(NPC, Bazaar)` per item (like BlingBlingAddons)
- Auto-refreshes prices every 5 minutes

### HUD Overlay
- Displays: Total Profit, $/hr, Session Time, Total Items
- Optional item breakdown showing top earners
- Configurable position, scale, and visibility
- Semi-transparent background
- Auto-pauses when idle (configurable timeout)

### Session Management
- Auto-starts when mining items are detected
- Auto-pauses after configurable idle timeout (default 60s)
- Subtracts idle time from session (like BigDiamond)
- Prints session summary to chat on pause
- Resumes automatically when new items detected

## Commands

| Command | Description |
|---|---|
| `/pt` | Show help |
| `/pt reset` | Reset current session |
| `/pt hud` | Toggle HUD on/off |
| `/pt move <x> <y>` | Set HUD position |
| `/pt scale <0.5-3.0>` | Set HUD scale |
| `/pt pricing <mode>` | `npc`, `bazaar_sell`, or `bazaar_buy` |
| `/pt timeout <seconds>` | Idle timeout (10-600s) |
| `/pt breakdown` | Toggle item breakdown display |
| `/pt stats` | Print session stats to chat |
| `/pt prices` | Force refresh Bazaar prices |
| `/pt webhook <url>` | Set Discord webhook URL |

## Building

### Prerequisites
- Java 21+
- Internet connection (for Gradle to download dependencies)

### Steps
```bash
# Clone or extract the project
cd SkyblockProfitTracker

# Build the mod (Linux/macOS)
./gradlew build

# Build the mod (Windows)
gradlew.bat build
```

The built `.jar` will be at `build/libs/SkyblockProfitTracker-1.0.0.jar`.

### Installing
1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.10
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (`0.138.3+1.21.10` or later)
3. Place `SkyblockProfitTracker-1.0.0.jar` in your `.minecraft/mods/` folder
4. Launch Minecraft 1.21.10 with Fabric

## Config

Configuration is saved to `.minecraft/config/skyblock-profit-tracker.json` and can be edited manually or via commands.

## Feature Mapping from Original Modules

| Original Feature | Source | Port Status |
|---|---|---|
| Sack message parsing + hover text | BigDiamond `trackers.js` | ✅ `ChatParser.java` |
| NPC ore prices | BigDiamond `utils.js` | ✅ `ItemPrices.java` |
| Profit/hr calculation | BigDiamond `profitTracker.js` | ✅ `ProfitSession.java` |
| Session timeout + auto-reset | BigDiamond `profitTracker.js` | ✅ `ProfitSession.java` |
| HUD overlay display | BigDiamond `gui.js` | ✅ `ProfitHudOverlay.java` |
| Draggable HUD position | BigDiamond `gui.js` | ✅ Via `/pt move` command |
| Discord webhook | BigDiamond `Discord/manager.js` | ✅ Config stored, sending TODO |
| PRISTINE! gemstone tracking | BlingBling `miningtracker.js` | ✅ `ChatParser.java` |
| Bazaar price fetching | BlingBling `mininginfo.js` | ✅ `BazaarPriceFetcher.java` |
| max(NPC, Bazaar) pricing | BlingBling `mininginfo.js` | ✅ `BazaarPriceFetcher.java` |
| Area detection (scoreboard) | BlingBling `helperFunctions.js` | ✅ `AreaDetector.java` |
| Enchanted item multipliers | BigDiamond `trackers.js` | ✅ `ChatParser.java` |
| Sell offer vs instant sell | BlingBling `mininginfo.js` | ✅ `/pt pricing` command |

## Requirements
- Minecraft 1.21.10
- Fabric Loader ≥ 0.17.0
- Fabric API
- Java 21+
