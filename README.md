# Blockgame

![Status](https://img.shields.io/badge/status-alpha-yellow)
![Java](https://img.shields.io/badge/Java-8%2B-red?logo=java)
![Maven](https://img.shields.io/badge/Built%20With-Maven-blue)
![Last Commit](https://img.shields.io/github/last-commit/djleamen/blockgame)
![OpenGL](https://img.shields.io/badge/Rendering-OpenGL-orange)
![Inspired By](https://img.shields.io/badge/inspired%20by-Minecraft-lightgrey?logo=minecraft)

<img width="663" height="500" alt="Screenshot 2025-07-27 at 7 53 53 PM" src="https://github.com/user-attachments/assets/b195945d-c0d2-40f6-a871-a9f2e1f662fc" />

*hey... I never claimed to be a good builder*

## Overview
This project is a super basic 3D block game with heavy inspiration taken from early alpha versions of Minecraft, implemented in Java. It includes fundamental functionalities such as player movement, block breaking/placing, world generation, and a complete block-based building system.

## Features
- **Player Movement & Physics**
  - WASD movement with mouse look
  - Gravity and jumping mechanics
  - Collision detection with blocks
  - Anti-stuck system (automatically frees player if trapped inside blocks)

- **Block System**
  - Multiple block types: Grass, Dirt, and Cobblestone
  - Realistic grass-to-dirt conversion (grass becomes dirt when covered, dirt becomes grass when exposed)
  - Procedurally generated textures (16x16)
  - Proper block rendering with appropriate textures

- **Hotbar & Inventory**
  - 9-slot hotbar system
  - Mouse scroll wheel or number keys (1-9) for block selection
  - Visual hotbar UI with block icons
  - Only place blocks when holding them (empty hand can't place blocks)

- **World Generation**
  - 64x64 world with 3-layer structure
  - Grass surface layer
  - Two cobblestone foundation layers underneath
  - Layered world geology for realistic mining

- **User Interface**
  - Crosshair for precise block targeting
  - Hotbar display showing available blocks
  - Clean, minimalist UI design

- **Building & Mining**
  - Left-click to break any block
  - Enter key to place selected block
  - 5-block reach distance
  - Cooldown system prevents spam clicking

## Controls

### Movement

- **WASD**: Move forward/backward/left/right
- **Mouse**: Look around (camera control)
- **Space**: Jump
- **Escape**: Exit game

### Building & Mining

- **Left Click**: Break blocks
- **Enter**: Place selected block
- **Mouse Scroll**: Change hotbar selection
- **Number Keys (1-9)**: Select hotbar slot directly

### Hotbar

- **Slot 1**: Grass blocks
- **Slot 2**: Dirt blocks  
- **Slot 3**: Cobblestone blocks
- **Slots 4-9**: Empty (available for future block types)

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

## Future Improvements

- Additional block types (stone, wood, etc.)
- Save/load world functionality
- Infinite world generation
- Inventory system with block collection
- Day/night cycle
- Lighting system
- Multiplayer support
- Water and physics simulation
