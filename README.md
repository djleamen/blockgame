# Blockgame

![Status](https://img.shields.io/badge/status-alpha-yellow)
![Java](https://img.shields.io/badge/Java-11-red?logo=java)
![Maven](https://img.shields.io/badge/Built%20With-Maven-blue)
![Last Commit](https://img.shields.io/github/last-commit/djleamen/blockgame)
![OpenGL](https://img.shields.io/badge/Rendering-OpenGL-orange)
![Inspired By](https://img.shields.io/badge/inspired%20by-Minecraft-lightgrey?logo=minecraft)

<img width="410" height="229" alt="Screenshot 2025-07-27 at 7 55 29 PM" src="https://github.com/user-attachments/assets/507a6046-4e9f-42b3-b4d2-2f500a159f84" />

*hey... I never claimed to be a good builder*

## Overview
This project is a super basic 3D block game with heavy inspiration taken from early alpha versions of Minecraft, implemented in Java. It includes fundamental functionalities such as player movement, block breaking/placing, world generation, and a complete block-based building system.

## Features
- **Player Movement & Physics**
  - WASD movement with mouse look
  - Delta-time-based gravity, jumping, and terminal velocity
  - Sprint (Ctrl) and sneak (Shift) speed modifiers
  - Collision detection with blocks
  - Anti-stuck system (automatically frees player if trapped inside blocks)

- **Block System**
  - 13 block types: Grass, Dirt, Cobblestone, Stone, Sand, Bedrock, Oak Log, Oak Planks, Oak Leaves, Water, Gravel, Coal Ore, Iron Ore
  - Realistic grass-to-dirt conversion (grass becomes dirt when covered, dirt becomes grass when exposed)
  - Still water fills terrain to sea level and renders translucent
  - Procedurally generated 16×16 textures built in memory at startup — no PNGs required
  - Per-face culling: only faces exposed to air are drawn
  - Bedrock is unbreakable

- **Hotbar & Inventory**
  - Survival-style collection: breaking a block adds its drop to your hotbar
    (stone drops cobblestone, grass drops dirt, leaves drop nothing)
  - Stacks up to 64 per slot with on-screen counts; placing consumes from the stack
  - 27-slot inventory storage — press **E** to open, click to move stacks
    between storage and the hotbar
  - Mouse scroll wheel or number keys (1-9) for block selection
  - Only place blocks when holding them (empty hand can't place blocks)

- **World Generation**
  - 64×64 world, up to 64 blocks tall
  - Value-noise heightmap terrain with hills, beaches and still-water seas
  - Bedrock floor, stone fill, dirt crust, grass/sand surface
  - Gravel patches on beaches, coal and iron ore veins in the stone layer
  - Small oak trees scattered across grass tiles
  - Deterministic seed so the world looks the same every launch

- **Day/Night & Persistence**
  - 10-minute day/night cycle — sky colour, fog and world brightness follow the clock
  - World autosaves to `world.bgsave` every 60 seconds and on exit, and resumes on launch

- **User Interface**
  - Crosshair for precise block targeting
  - Wireframe outline on whichever block the crosshair is currently on
  - Hotbar display showing available blocks
  - Clean, minimalist UI design

- **Building & Mining**
  - Left-click to break any block (except bedrock)
  - Right-click (or Enter) to place selected block
  - 5-block reach distance
  - Cooldown system prevents spam clicking

## Controls

### Movement

- **WASD**: Move forward/backward/left/right
- **Mouse**: Look around (camera control)
- **Space**: Jump
- **Left Ctrl**: Sprint
- **Left Shift**: Sneak (slower)
- **Escape**: Exit game

### Building & Mining

- **Left Click**: Break blocks
- **Right Click** (or **Enter**): Place selected block
- **Mouse Scroll**: Change hotbar selection
- **Number Keys (1-9)**: Select hotbar slot directly
- **E**: Open/close the inventory screen (click to move stacks)

### Inventory

The hotbar starts empty — break blocks to collect them. When the hotbar
fills up, further pickups overflow into the 27-slot storage. Press **E**
and click a slot to pick up a stack, click again to place, merge or swap it.

## Setup Instructions

1. Clone the repository:

   ```bash
   git clone https://github.com/djleamen/blockgame.git
   ```

2. Navigate to the project directory:

   ```bash
   cd blockgame
   ```

3. Build the project using Maven:

   ```bash
   mvn clean install
   mvn clean package
   ```

4. Run the game:

   ```bash
   java -jar target/mc-clone-1.0-SNAPSHOT-shaded.jar
   ```

   Mac Users:

   ```bash
   java -XstartOnFirstThread -jar target/mc-clone-1.0-SNAPSHOT-shaded.jar
   ```

## Requirements

- Java Development Kit (JDK) 8 or higher
- Maven

## Roadmap

The detailed multi-phase plan — including which existing GitHub issues each
phase closes — lives in [ROADMAP.md](ROADMAP.md). Highlights:

- **Phase 2** — chunk container, inventory pickup, water/gravel/ores, basic tools
- **Phase 3** — save/load to `.bgsave` ([#2](https://github.com/djleamen/blockgame/issues/2))
- **Phase 4** — day/night cycle + lighting ([#5](https://github.com/djleamen/blockgame/issues/5), [#6](https://github.com/djleamen/blockgame/issues/6))
- **Phase 5** — streaming chunks for infinite worlds ([#3](https://github.com/djleamen/blockgame/issues/3))
- **Phase 6** — swept-AABB collision and water/lava physics ([#8](https://github.com/djleamen/blockgame/issues/8))
- **Phase 7** — multiplayer ([#7](https://github.com/djleamen/blockgame/issues/7))
