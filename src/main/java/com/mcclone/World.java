/*
 * This file represents the world in the game.
 * It handles the rendering of the world, including the blocks and their textures.
 * It also handles the world generation and the ground height.
 * Currently, the world is a flat plane with a fixed size.
 */

 package com.mcclone;

 import org.lwjgl.opengl.GL11;
 
 public class World {
     public static final int SIZE = 64;
     public static final int BLOCK_TYPE_AIR = 0;
     public static final int BLOCK_TYPE_GRASS = 1;
     public static final int BLOCK_TYPE_DIRT = 2;
     public static final int BLOCK_TYPE_COBBLESTONE = 3;
     public static final int BLOCK_TYPE_PLACED_DIRT = 4;
     
     private final Block block = new Block();
 
     private final int[][][] blocks = new int[SIZE][SIZE][SIZE];
 
     public World() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                blocks[x][0][z] = BLOCK_TYPE_COBBLESTONE; // Bottom layer
                blocks[x][1][z] = BLOCK_TYPE_COBBLESTONE; // Middle layer
                blocks[x][2][z] = BLOCK_TYPE_GRASS;       // Top layer (surface)
            }
        }
    }
 
    public void render() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (blocks[x][y][z] == BLOCK_TYPE_AIR) continue;
    
                    GL11.glPushMatrix();
                    GL11.glTranslatef(x - SIZE / 2f, y, -z);
                    
                    switch (blocks[x][y][z]) {
                        case BLOCK_TYPE_GRASS -> block.renderAsGrass();
                        case BLOCK_TYPE_DIRT, BLOCK_TYPE_PLACED_DIRT -> block.renderAsDirt();
                        case BLOCK_TYPE_COBBLESTONE -> block.renderAsCobblestone();
                        default -> {
                            // No action for unknown block types
                        }
                    }
                    
                    GL11.glPopMatrix();
                }
            }
        }
    }
 
    public float groundHeight(float x, float z) {
        int bx = (int) Math.floor(x + SIZE / 2f);
        int bz = (int) Math.floor(-z);
    
        for (int y = SIZE-1; y >= 0; y--) {
            if (hasBlock(bx, y, bz)) return y + 1f;
        }
        return 0f;
    }
  
     public void breakBlock(int x, int y, int z) {
        if (inBounds(x, y, z)) {
            blocks[x][y][z] = BLOCK_TYPE_AIR;
            updateGrassBlocks();
        }
    }
    
    public void placeBlock(int x, int y, int z) {
        if (inBounds(x, y, z) && blocks[x][y][z] == BLOCK_TYPE_AIR) {
            blocks[x][y][z] = BLOCK_TYPE_GRASS; // new blocks placed are grass by default
            updateGrassBlocks();
        }
    }
    
    public void placeBlockOfType(int x, int y, int z, int blockType) {
        if (inBounds(x, y, z) && blocks[x][y][z] == BLOCK_TYPE_AIR) {
            blocks[x][y][z] = blockType;
            updateGrassBlocks();
        }
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }
 
     public boolean hasBlock(int x, int y, int z) {
        return inBounds(x, y, z) && blocks[x][y][z] != BLOCK_TYPE_AIR;
    }
    
    public int getBlockType(int x, int y, int z) {
        if (inBounds(x, y, z)) {
            return blocks[x][y][z];
        }
        return BLOCK_TYPE_AIR;
    }
    
    // update grass blocks to dirt if they have a block above them
    // note: this only affects natural grass/dirt conversion, not placed dirt blocks
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