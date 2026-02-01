/**
 * This file represents the world in the game.
 * It handles the rendering of the world, including the blocks and their textures.
 * It also handles the world generation and the ground height.
 * Currently, the world is a flat plane with a fixed size.
 */

 package com.mcclone;

 import org.lwjgl.opengl.GL11;
 
 /**
  * Represents the game world containing all blocks.
  * Manages block data, rendering, and world generation.
  * Currently implements a fixed-size 64x64x64 block world with a flat terrain.
  */
 public class World {
     /** World size in blocks (64x64x64) */
     public static final int SIZE = 64;
     
     /** Block type constant for air (empty space) */
     public static final int BLOCK_TYPE_AIR = 0;
     
     /** Block type constant for grass blocks */
     public static final int BLOCK_TYPE_GRASS = 1;
     
     /** Block type constant for naturally generated dirt */
     public static final int BLOCK_TYPE_DIRT = 2;
     
     /** Block type constant for cobblestone */
     public static final int BLOCK_TYPE_COBBLESTONE = 3;
     
     /** Block type constant for player-placed dirt (doesn't convert to grass) */
     public static final int BLOCK_TYPE_PLACED_DIRT = 4;
     
     /** Block renderer instance */
     private final Block block = new Block();
 
     /** 3D array storing block types [x][y][z] */
     private final int[][][] blocks = new int[SIZE][SIZE][SIZE];
 
     /**
      * Constructs a new World with flat terrain.
      * Generates a 3-layer structure: 2 layers of cobblestone foundation and 1 layer of grass surface.
      */
     public World() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                blocks[x][0][z] = BLOCK_TYPE_COBBLESTONE; // Bottom layer
                blocks[x][1][z] = BLOCK_TYPE_COBBLESTONE; // Middle layer
                blocks[x][2][z] = BLOCK_TYPE_GRASS;       // Top layer (surface)
            }
        }
    }
 
    /**
     * Renders all non-air blocks in the world.
     * Iterates through all block positions and renders each block with appropriate textures.
     */
    public void render() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (blocks[x][y][z] == BLOCK_TYPE_AIR) {
                        continue;
                    }
    
                    GL11.glPushMatrix();
                    GL11.glTranslatef(x - SIZE / 2f, y, -z);
                    
                    switch (blocks[x][y][z]) {
                        case BLOCK_TYPE_GRASS:
                            block.renderAsGrass();
                            break;
                        case BLOCK_TYPE_DIRT:
                        case BLOCK_TYPE_PLACED_DIRT:
                            block.renderAsDirt();
                            break;
                        case BLOCK_TYPE_COBBLESTONE:
                            block.renderAsCobblestone();
                            break;
                        default:
                            // No action for unknown block types
                            break;
                    }
                    
                    GL11.glPopMatrix();
                }
            }
        }
    }
 
    /**
     * Calculates the ground height at a specific world position.
     * Searches from top to bottom for the first solid block.
     * 
     * @param x the X coordinate in world space
     * @param z the Z coordinate in world space
     * @return the Y coordinate of the ground surface, or 0 if no blocks found
     */
    public float groundHeight(float x, float z) {
        int bx = (int) Math.floor(x + SIZE / 2f);
        int bz = (int) Math.floor(-z);
    
        for (int y = SIZE-1; y >= 0; y--) {
            if (hasBlock(bx, y, bz)) {
                return y + 1f;
            }
        }
        return 0f;
    }
  
     /**
      * Breaks (removes) a block at the specified position.
      * Updates grass/dirt conversions after removal.
      * 
      * @param x the block X coordinate
      * @param y the block Y coordinate
      * @param z the block Z coordinate
      */
     public void breakBlock(int x, int y, int z) {
        if (inBounds(x, y, z)) {
            blocks[x][y][z] = BLOCK_TYPE_AIR;
            updateGrassBlocks();
        }
    }
    
    /**
     * Places a grass block at the specified position.
     * Only places if the position is currently air.
     * 
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     */
    public void placeBlock(int x, int y, int z) {
        if (inBounds(x, y, z) && blocks[x][y][z] == BLOCK_TYPE_AIR) {
            blocks[x][y][z] = BLOCK_TYPE_GRASS; // new blocks placed are grass by default
            updateGrassBlocks();
        }
    }
    
    /**
     * Places a block of the specified type at the given position.
     * Only places if the position is currently air.
     * 
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @param blockType the type of block to place
     */
    public void placeBlockOfType(int x, int y, int z, int blockType) {
        if (inBounds(x, y, z) && blocks[x][y][z] == BLOCK_TYPE_AIR) {
            blocks[x][y][z] = blockType;
            updateGrassBlocks();
        }
    }

    /**
     * Checks if the given coordinates are within world bounds.
     * 
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @return true if coordinates are valid, false otherwise
     */
    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }
 
     /**
      * Checks if there is a solid block at the specified position.
      * 
      * @param x the block X coordinate
      * @param y the block Y coordinate
      * @param z the block Z coordinate
      * @return true if a non-air block exists, false otherwise
      */
     public boolean hasBlock(int x, int y, int z) {
        return inBounds(x, y, z) && blocks[x][y][z] != BLOCK_TYPE_AIR;
    }
    
    /**
     * Gets the block type at the specified position.
     * 
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @return the block type ID, or BLOCK_TYPE_AIR if out of bounds
     */
    public int getBlockType(int x, int y, int z) {
        if (inBounds(x, y, z)) {
            return blocks[x][y][z];
        }
        return BLOCK_TYPE_AIR;
    }
    
    /**
     * Updates all grass/dirt blocks based on whether they're covered.
     * Grass blocks with blocks above them convert to dirt.
     * Dirt blocks exposed to air convert back to grass.
     * Note: BLOCK_TYPE_PLACED_DIRT is never converted automatically.
     */
    public void updateGrassBlocks() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE - 1; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (blocks[x][y][z] == BLOCK_TYPE_GRASS && 
                        blocks[x][y + 1][z] != BLOCK_TYPE_AIR) {
                        blocks[x][y][z] = BLOCK_TYPE_DIRT;
                    }
                    else if (blocks[x][y][z] == BLOCK_TYPE_DIRT && 
                             blocks[x][y + 1][z] == BLOCK_TYPE_AIR) {
                        blocks[x][y][z] = BLOCK_TYPE_GRASS;
                    }
                    // note: BLOCK_TYPE_PLACED_DIRT is never converted automatically
                }
            }
        }
    }
 }