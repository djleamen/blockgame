/*
 * This file represents the world in the game.
 * It handles the rendering of the world, including the blocks and their textures.
 * It also handles the world generation and the ground height.
 * Currently, the world is a flat plane with a fixed size.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;

public class World {
    private static final int SIZE = 64; // single “chunk” is 64 × 1 × 64 grass blocks
    private final Block block = new Block();

    public void render() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {

                boolean edge = (x == 0 || x == SIZE - 1 || z == 0 || z == SIZE - 1);

                GL11.glPushMatrix(); 
                // centre X but let Z run from 0 to -SIZE so ground starts right under spawn
                GL11.glTranslatef(x - SIZE / 2f,0,-z);
                block.renderTop();
                if (edge) block.renderSides();
                GL11.glPopMatrix();
            }
        }
    }

    // flat world = constant ground height
    public float groundHeight(float x, float z) { return 1f; }
}