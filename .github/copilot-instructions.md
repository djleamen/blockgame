# Copilot Instructions for Blockgame

## Project Overview

Blockgame is a basic 3D block game implementation in Java, heavily inspired by early alpha versions of Minecraft. It features player movement, block breaking/placing, world generation, and a complete block-based building system using OpenGL for rendering and LWJGL (Lightweight Java Game Library) for window management and input handling.

## Technology Stack

- **Language**: Java (minimum JDK 8, pom.xml configured for Java 21)
- **Build Tool**: Maven
- **Graphics**: OpenGL via LWJGL 3.3.2
- **Window Management**: GLFW (via LWJGL)
- **Rendering**: OpenGL with custom texture generation
- **Architecture**: Single-threaded game loop with immediate mode OpenGL

## Project Structure

```
blockgame/
├── src/main/java/com/mcclone/
│   ├── Game.java              # Main game loop, window, input, and rendering
│   ├── Player.java            # Player movement, physics, and camera
│   ├── World.java             # World data, block management, and block updates
│   ├── Block.java             # Block type definitions and properties
│   ├── Hotbar.java            # Hotbar UI and inventory management
│   ├── TextureGenerator.java # Procedural texture generation (16x16)
│   ├── TextureLoader.java    # OpenGL texture loading and management
│   └── TextureLoadException.java # Custom exception for texture errors
├── pom.xml                    # Maven configuration
└── README.md                  # User-facing documentation
```

## Build and Run Instructions

### Building the Project

```bash
# Clean and compile
mvn clean compile

# Create shaded JAR with all dependencies
mvn clean package
```

### Running the Game

```bash
# Standard (Linux/Windows)
java -jar target/mc-clone-1.0-SNAPSHOT-shaded.jar

# macOS (requires -XstartOnFirstThread for LWJGL)
java -XstartOnFirstThread -jar target/mc-clone-1.0-SNAPSHOT-shaded.jar
```

### Maven Commands

- `mvn clean compile` - Compile the project
- `mvn clean package` - Build the shaded JAR
- `mvn clean install` - Install to local Maven repository

## Key Dependencies

- **LWJGL 3.3.2**: Core library, GLFW, OpenGL, and STB modules
- **Native Libraries**: macOS ARM64 natives included (expand for other platforms as needed)

## Code Conventions

### General Guidelines

1. **OpenGL Context**: All OpenGL calls must be made from the main thread
2. **Coordinate System**: 
   - World coordinates use standard 3D space (x, y, z)
   - Y-axis is vertical (up/down)
   - Block positions are integers, player position is floating-point
3. **Block Types**: Defined in `Block.java` enum (GRASS, DIRT, COBBLESTONE)
4. **Texture IDs**: Managed by `TextureLoader`, referenced by block type

### Naming Conventions

- **Classes**: PascalCase (e.g., `TextureGenerator`, `Game`)
- **Methods**: camelCase (e.g., `updatePlayer`, `renderBlock`)
- **Constants**: UPPER_SNAKE_CASE for true constants
- **Variables**: camelCase for local and instance variables

### Code Style

- **Indentation**: Consistent indentation (appears to be 4 spaces)
- **Braces**: Opening brace on same line
- **Comments**: Use block comments for file-level documentation, inline comments for complex logic
- **Imports**: Static imports for frequently used OpenGL/GLFW constants

### Important Patterns

1. **Game Loop**: Located in `Game.java` - follows standard game loop pattern:
   - Process input
   - Update game state
   - Render frame
   - Swap buffers

2. **OpenGL Usage**: Uses immediate mode (GL_QUADS) for block rendering
   - Each block face is rendered as a separate quad
   - Textures are bound before rendering each block type

3. **Player Physics**: 
   - Gravity constant: implemented in `Player.java`
   - Collision detection with blocks
   - Anti-stuck system to prevent player getting trapped

4. **World Management**:
   - Fixed world size (64x64 blocks)
   - 3-layer total structure (1 grass surface layer + 2 cobblestone foundation layers)
   - Grass-to-dirt conversion logic when blocks are placed above

## Testing

**Note**: This project currently has no automated test suite. When adding tests:
- Create test files in `src/test/java/com/mcclone/`
- Use JUnit for unit tests
- Focus on non-graphics logic (physics, world generation, collision detection)
- Graphics/OpenGL code is difficult to test automatically

## Common Development Tasks

### Adding a New Block Type

1. Add enum value to `Block.java`
2. Generate texture in `TextureGenerator.java`
3. Load texture in `TextureLoader.java`
4. Update hotbar in `Hotbar.java` if needed
5. Update rendering logic in `World.java` if block has special properties

### Modifying Player Controls

- Edit input handling in `Game.java` (keyboard and mouse callbacks)
- Update player movement logic in `Player.java`
- Document control changes in `README.md`

### Changing World Generation

- Modify world initialization in `World.java` constructor
- Adjust world size constants as needed
- Update terrain generation algorithm

## Platform-Specific Notes

### macOS
- **Critical**: Must use `-XstartOnFirstThread` JVM argument
- Native libraries for ARM64 included in dependencies
- Intel Macs may need different LWJGL natives

### Linux/Windows
- Standard JVM execution works fine
- May need to add platform-specific LWJGL natives to `pom.xml`

## Dependencies and Security

- Keep LWJGL version updated for security patches
- All dependencies are managed through Maven
- Shaded JAR includes all dependencies for distribution

## Performance Considerations

1. **Rendering**: Immediate mode OpenGL is used with deprecated features (GL_QUADS). This is not optimal for modern hardware and is not available in modern OpenGL core profiles. Future optimization could involve migrating to vertex buffer objects (VBOs) and modern OpenGL.
2. **World Size**: Fixed 64x64 size keeps memory usage low
3. **Texture Generation**: Done at startup, cached in OpenGL texture memory
4. **Frame Rate**: VSync enabled (60 FPS cap)

## Future Enhancement Areas

As documented in README.md, potential improvements include:
- Additional block types
- Save/load functionality
- Infinite world generation (chunks)
- Inventory system with block collection
- Day/night cycle and lighting
- Multiplayer support
- Water physics

## Important Notes for Code Modifications

1. **OpenGL Thread Safety**: Never call OpenGL functions from threads other than the main thread
2. **Resource Cleanup**: Ensure textures and OpenGL resources are properly freed on shutdown
3. **Coordinate Systems**: Be careful with coordinate transformations (world vs. screen space)
4. **macOS Compatibility**: Always test or note when changes might affect `-XstartOnFirstThread` requirement
5. **Performance**: Keep in mind the immediate mode rendering approach when adding features
6. **Block Updates**: Remember to handle grass-to-dirt conversion when adding block manipulation features

## Getting Help

- README.md contains user-facing documentation and setup instructions
- Code comments explain complex rendering and physics logic
- LWJGL documentation: https://www.lwjgl.org/guide
- OpenGL reference: https://www.khronos.org/opengl/
