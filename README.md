# Blockgame

## Overview
This project is a super basic 3D block game with heavy inspiration taken from early alpha versions of Minecraft, implemented in Java. It includes fundamental functionalities such as player movement, block breaking/placing, and world generation.

## Features
- Player movement
- Simple game world generation

## Setup Instructions
1. Clone the repository:
   ```
   git clone https://github.com/djleamen/blockgame.git
   ```
2. Navigate to the project directory:
   ```
   cd blockgame
   ```
3. Build the project using Maven:
   ```
   mvn clean install
   mvn clean package
   ```
4. Run the game:
   ```
   java -jar target/mc-clone-1.0-SNAPSHOT-shaded.jar
   ```
   Mac Users:
   ```
   java -XstartOnFirstThread -jar target/mc-clone-1.0-SNAPSHOT-shaded.jar
   ```

## Requirements
- Java Development Kit (JDK) 8 or higher
- Maven

## Future Improvements
- Add block textures
- Add more block types
- Enhance world generation algorithms
- Improve graphics and rendering techniques
