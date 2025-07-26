/*
 * This file represents a Player in the game.
 * It handles the player's position, movement, and camera controls.
 * It also handles the player's physics, including gravity and jumping.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;

public class Player {

    private static final float GRAVITY = 0.008f;
    private static final float EYE = 1.62f;

    public float x,y,z;
    private float pitch,yaw;
    private float vy;

    public Player(float x,float y,float z){this.x=x;this.y=y;this.z=z;}

    // physics
    public void tickPhysics(World world) {
        vy -= GRAVITY;
        float newY = y + vy;
    
        // Check collision at current x,z position for the new y position
        float ground = world.groundHeight(x, z) + 2.0f; // player is 2 blocks tall
    
        // Only land on ground if falling down and close to ground level
        if (vy <= 0 && newY <= ground + 0.1f) {
            y = ground;
            vy = 0;
        } else {
            // Otherwise, apply normal physics movement
            y = newY;
        }
    }
    public boolean onGround(){return vy==0;}
    
    public void move(float dx, float dz, World world) {
        float newX = x + dx;
        float newZ = z + dz;

        // check collision at new position
        if (canMoveTo(newX, newZ, world)) {
            x = newX;
            z = newZ;
        }
    }
    
    private boolean canMoveTo(float newX, float newZ, World world) {
        // player collision box dimensions
        final float PLAYER_WIDTH = 0.6f;
        final float PLAYER_HEIGHT = 2.0f;
        final float COLLISION_BUFFER = 0.1f;
        
        // calculate player's bounding box at new position
        float minX = newX - PLAYER_WIDTH/2 - COLLISION_BUFFER;
        float maxX = newX + PLAYER_WIDTH/2 + COLLISION_BUFFER;
        float minZ = newZ - PLAYER_WIDTH/2 - COLLISION_BUFFER;
        float maxZ = newZ + PLAYER_WIDTH/2 + COLLISION_BUFFER;
        float minY = y - PLAYER_HEIGHT;
        float maxY = y;
        
        // convert to block coordinates and check all blocks the player would overlap with
        int minBlockX = (int) Math.floor(minX + World.SIZE / 2f);
        int maxBlockX = (int) Math.floor(maxX + World.SIZE / 2f);
        int minBlockZ = (int) Math.floor(-maxZ);
        int maxBlockZ = (int) Math.floor(-minZ);
        int minBlockY = (int) Math.floor(minY);
        int maxBlockY = (int) Math.floor(maxY);
        
        // check for collision with any block in the player's bounding box
        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                for (int by = minBlockY; by <= maxBlockY; by++) {
                    if (world.hasBlock(bx, by, bz)) {
                        return false;
                    }
                }
            }
        }
        
        // climbing logic
        float currentGround = world.groundHeight(x, z);
        float newGround = world.groundHeight(newX, newZ);
        float heightDiff = newGround - currentGround;
        
        if (heightDiff > 1.0f && vy <= 0) {
            return false;
        }

        return true;
    }
    
    public void jump(float v){if(onGround())vy=v;}

    // camera
    public void addYaw(float d){yaw+=d;}
    public void addPitch(float d){pitch=Math.max(-89,Math.min(89,pitch+d));}
    public float getYaw(){return yaw;}
    public float getPitch(){return pitch;}

    public void applyCamera(){
        GL11.glRotatef(-pitch,1,0,0);
        GL11.glRotatef( yaw ,0,1,0);
        GL11.glTranslatef(-x,-y,-z);
    }

    public static float getEYE() {
        return EYE;
    }
}