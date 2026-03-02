# Skyblock Profit Tracker — Fabric 1.21.10

A client-side Fabric mod that tracks ore and gemstone mining profits in Hypixel SkyBlock. (msg dwjdw on dc for issues)

## Credits

Based on the following ChatTriggers modules, ported to a modern Fabric mod:

- **[bigdiamond](https://github.com/eatpIastic/bigdiamond)** — Ore tracking (sack message parsing, enchanted item multipliers, session management, idle timeout)
- **[BlingBlingAddons](https://github.com/blingblingdeveloper/BlingBlingAddons)** — Gemstone tracking (PRISTINE! detection, Bazaar price fetching, max(NPC, Bazaar) pricing)
- **[NoammAddons](https://github.com/Noamm9/NoammAddons-1.21.10)** — GUI and HUD (settings screen, HUD overlay, HSB color picker, draggable HUD editor)

## Commands

All commands use `/pt` (or `/profittracker`).

### General

| Command | Description |
|---|---|
| `/pt` | Print all available commands to chat. |
| `/pt gui` | Open the settings GUI. Configure pricing mode, gemstone rarity, HUD colors, toggle individual HUD elements, set breakdown item count, and idle timeout — all from one screen. |
| `/pt edit` | Open the HUD editor. Drag the HUD overlay to reposition it on screen. |
| `/pt reset` | Clear all session data (profit, items, time) and start fresh. |
| `/pt stats` | Print current session stats to chat: total profit, $/hr, session time, ore count, and gem count. |

### HUD

| Command | Description |
|---|---|
| `/pt hud` | Toggle the HUD overlay on or off. |
| `/pt move <x> <y>` | Set the HUD position to exact pixel coordinates. Both values must be >= 0. |
| `/pt scale <0.5-3.0>` | Set the HUD scale. Values outside the range are clamped. |
| `/pt breakdown` | Toggle the item breakdown section on the HUD, which shows your top-earning items and their individual profit. |

### Pricing

| Command | Description |
|---|---|
| `/pt pricing <mode>` | Set the pricing mode. Options: `npc` (static NPC sell prices), `bazaar_sell` (Bazaar Instant Sell — real-time, default), `bazaar_buy` (Bazaar Sell Offer — buy order prices, higher estimates). Switching to a Bazaar mode triggers an immediate price refresh. |
| `/pt prices` | Force refresh all Bazaar prices immediately. Prices auto-refresh every 5 minutes. |
| `/pt setprice <item> <price>` | Override the price of a specific item. Accepts ore names (`diamond`, `coal`, `mithril`, etc.) and gemstone names (`ruby`, `jade`, etc.). The custom price overrides all pricing modes for that item. Price must be >= 0. |
| `/pt clearprice <item>` | Remove a custom price override for an item, reverting it to the active pricing mode. |
| `/pt listprices` | List all custom price overrides currently set. |

### Gemstones

| Command | Description |
|---|---|
| `/pt gemstone <rarity>` | Set which gemstone rarity to price. Options: `flawed` (tier 1), `fine` (tier 2), `flawless` (tier 3). This determines which Bazaar product ID is used when fetching gemstone prices. |

### Session

| Command | Description |
|---|---|
| `/pt timeout <10-600>` | Set the idle timeout in seconds. When no mining activity is detected for this duration, the session auto-pauses and idle time is subtracted from the session. Ore tracking always uses a minimum of 60s regardless of this setting. |

## Tracked Items

### Ores
Diamond, Coal, Iron, Gold, Emerald, Lapis, Redstone, Obsidian, Mithril, Titanium, End Stone, Glacite, Umber, Tungsten, Quartz.

Detected from `[Sacks] +X items. (Last Ys.)` chat messages. Hover text is parsed for individual item types. Enchanted items are multiplied by 160, enchanted blocks by 160x160.

### Gemstones
Ruby, Amethyst, Jade, Sapphire, Amber, Topaz, Jasper, Aquamarine, Citrine, Peridot, Onyx.

Detected from `PRISTINE! You found a Flawed X Gemstone x#!` chat messages.

## Building

Requires Java 21+.

```bash
./gradlew build
```

Output: `build/libs/dWA-1.1.0.jar`

## Installing

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.10
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (0.138.3+1.21.10 or later)
3. Drop the `.jar` into `.minecraft/mods/`

## Config

Saved to `.minecraft/config/skyblock-profit-tracker.json`. Editable via `/pt gui` or manually.

## Requirements

- Minecraft 1.21.10
- Fabric Loader >= 0.17.0
- Fabric API
- Java 21+
