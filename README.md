# wavertp

[![Build & Release](https://github.com/wavestudio/wavertp/actions/workflows/build.yml/badge.svg)](https://github.com/wavestudio/wavertp/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/wavestudio/wavertp)](https://github.com/wavestudio/wavertp/releases)
[![Paper Version](https://img.shields.io/badge/Paper-1.21.7%2B-blue)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)

High-performance 3-dimension Random Teleport plugin for PaperMC 1.21.7+ (API 26.3) with native Paper Dialog API support.

## Features

- **Paper Dialog API** - Native confirmation dialog with dimension selection
- **3 Dimensions** - Overworld, Nether, The End with dimension-specific safety logic
- **Async Location Finding** - Thread-safe chunk access, no main thread lag
- **Configurable** - Cooldown, search radius, attempts, biome/block filters
- **Customizable Messages** - Full MiniMessage support via `messages.yml`
- **Item Display** - Real items in dialog body with tooltips (no resource pack needed)
- **Teleport Effects** - Sound and particle feedback

## Requirements

- PaperMC 1.21.7+ (API 26.3)
- Java 21

## Installation

1. Download the latest release JAR from [Releases](https://github.com/wavestudio/wavertp/releases)
2. Place in `plugins/` folder
3. Restart server
4. Configure `config.yml` and `messages.yml` as needed

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/rtp` | `wavertp.use` | Open RTP dialog |
| `/rtp reload` | `wavertp.admin` | Reload configuration |
| `/rtp cooldown <set\|clear\|check> [player]` | `wavertp.admin` | Manage cooldowns |

## Default Permissions

```yaml
permissions:
  wavertp.use:
    default: true
    description: Use /rtp command
  wavertp.admin:
    default: op
    description: Admin commands (reload, cooldown management)
```

## Configuration

### config.yml

```yaml
cooldown-seconds: 10
max-attempts: 20
search-radius: 10000

worlds:
  overworld: "world"
  nether: "world_nether"
  end: "world_the_end"

overworld:
  avoid-biomes:
    - "ocean"
    - "deep_ocean"
  avoid-blocks:
    - "water"
    - "lava"
    - "fire"
  required-air-blocks: 2

nether:
  min-y: 33
  max-y: 126
  avoid-blocks:
    - "lava"
    - "fire"
    - "magma_block"
  required-air-blocks: 2

end:
  min-y: 1
  valid-ground-blocks:
    - "end_stone"
    - "end_stone_bricks"
  required-air-blocks: 2

effects:
  sound: "ENTITY_ENDERMAN_TELEPORT"
  source-particle: "PORTAL"
  dest-particle: "END_ROD"
  particle-count: 30
```

### messages.yml

All user-facing messages with MiniMessage support:

```yaml
prefix: "<gradient:#00FF88:#00FFFF><bold>RTP</bold></gradient> <dark_gray>»</dark_gray> "

dialog:
  title: "<gradient:#FFD700:#FFA500><bold>Random Teleport</bold></gradient>"
  body: |
    <gray>Select a dimension to teleport to.</gray>
  dimensions:
    overworld: "<green>🌍 Overworld</green> <gray>(Surface)</gray>"
    nether: "<red>🌋 Nether</red> <gray>(Dangerous)</gray>"
    end: "<aqua>💎 The End</aqua> <gray>(Void islands)</gray>"

# ... more messages
```

## Dimension Safety Logic

### Overworld
- Finds highest solid block at random X,Z
- Avoids ocean biomes and water/lava/fire blocks
- Requires 2 air blocks above ground

### Nether
- Searches between Y=33 and Y=126 (avoids lava lakes and bedrock roof)
- Finds air pockets on solid ground
- Avoids lava, fire, magma blocks

### The End
- Searches only on end stone/end stone bricks/purpur
- Avoids void (Y < 1)
- Requires 2 air blocks above ground

## Building

### Local Build (requires Maven + Java 21)
```bash
mvn clean package
```
Output: `target/wavertp-1.0.0.jar`

### Automated Build (GitHub Actions)
1. Push to GitHub
2. Create a tag: `git tag v1.0.0 && git push origin v1.0.0`
3. GitHub Actions builds & creates Release with JAR automatically

## Architecture

```
com.wavestudio.rtp
├── RtpPlugin              # Main plugin class
├── config/
│   ├── RtpConfig          # Config + messages loader
│   └── MessageProvider    # MiniMessage parser with placeholders
├── command/
│   └── RtpCommand         # Command executor + tab completer
├── dialog/
│   └── RtpDialogFactory   # Paper Dialog API factory
├── rtp/
│   ├── SafeLocationFinder # Coordinator
│   ├── DimensionStrategy  # Interface
│   ├── OverworldStrategy  # Overworld logic
│   ├── NetherStrategy     # Nether logic
│   └── EndStrategy        # End logic
├── util/
│   └── CooldownManager    # Per-player cooldown tracking
└── model/
    └── Dimension          # Dimension enum
```

## License

MIT - Created by [wavestudio](https://github.com/wavestudio)