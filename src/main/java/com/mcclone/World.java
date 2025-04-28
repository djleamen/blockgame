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
     private final Block block = new Block();
 
     private final boolean[][][] blocks = new boolean[SIZE][SIZE][SIZE];
 
     public World() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                blocks[x][0][z] = true;
            }
        }
    }
 
    public void render() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (!blocks[x][y][z]) continue;
    
                    GL11.glPushMatrix();
                    GL11.glTranslatef(x - SIZE / 2f, y, -z);
                    block.render();
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
            blocks[x][y][z] = false;
        }
    }
    
    public void placeBlock(int x, int y, int z) {
        if (inBounds(x, y, z) && !blocks[x][y][z]) {
            blocks[x][y][z] = true;
        }
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }
 
     public boolean hasBlock(int x, int y, int z) {
        return inBounds(x, y, z) && blocks[x][y][z];
    }
 }