/*
 * This file represents a Player in the game.
 * It handles the player's position, movement, and camera controls.
 * It also handles the player's physics, including gravity and jumping.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Player {

    private static final Logger logger = LoggerFactory.getLogger(Player.class);
    private static final float GRAVITY = 0.008f;
    private static final float EYE = 1.62f;

    private float x;
    private float y;
    private float z;
    private float pitch;
    private float yaw;
    private float vy;

    public Player(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // physics
    public void tickPhysics(World world) {
        vy -= GRAVITY;
        float newY = y + vy;
    
        // check collision at current x,z position for the new y position
        // y represents eye level, so ground level for eyes is groundHeight + EYE
        float ground = world.groundHeight(x, z) + EYE;
    
        // only land on ground if falling down and close to ground level
        if (vy <= 0 && newY <= ground + 0.1f) {
            y = ground;
            vy = 0;
        } else {
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
        if (collidesAt(newX, y, newZ, world)) {
            return false;
        }
        return canClimbTo(newX, newZ, world);
    }

    private boolean collidesAt(float testX, float testY, float testZ, World world) {
        final float PLAYER_WIDTH = 0.6f;
        final float PLAYER_HEIGHT = 2.0f;
        final float COLLISION_BUFFER = 0.1f;
        final float EPSILON = 0.001f;

        float minX = testX - PLAYER_WIDTH/2 - COLLISION_BUFFER;
        float maxX = testX + PLAYER_WIDTH/2 + COLLISION_BUFFER;
        float minZ = testZ - PLAYER_WIDTH/2 - COLLISION_BUFFER;
        float maxZ = testZ + PLAYER_WIDTH/2 + COLLISION_BUFFER;
        float minY = testY - EYE;
        float maxY = testY + (PLAYER_HEIGHT - EYE);

        int minBlockX = (int) Math.floor(minX + World.SIZE / 2f);
        int maxBlockX = (int) Math.floor(maxX + World.SIZE / 2f);
        int minBlockZ = (int) Math.floor(-maxZ);
        int maxBlockZ = (int) Math.floor(-minZ);
        int minBlockY = (int) Math.floor(minY + EPSILON);
        int maxBlockY = (int) Math.floor(maxY - EPSILON);

        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                for (int by = minBlockY; by <= maxBlockY; by++) {
                    if (collidesWithBlock(bx, by, bz, minY, maxY, world)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean collidesWithBlock(int bx, int by, int bz, float minY, float maxY, World world) {
        if (world.hasBlock(bx, by, bz)) {
            float blockMinY = by;
            float blockMaxY = by + 1.0f;
            if (maxY > blockMinY && minY < blockMaxY) {
                if (logger.isDebugEnabled()) {
                    logger.debug("REAL COLLISION: Block at ({},{},{}), Player Y:{}, Feet:{}, Head:{}, BlockY:{}-{}",
                            bx, by, bz, String.format("%.3f", y), String.format("%.3f", minY), 
                            String.format("%.3f", maxY), String.format("%.1f", blockMinY), String.format("%.1f", blockMaxY));
                }
                return true;
            }
        }
        return false;
    }

    private boolean canClimbTo(float newX, float newZ, World world) {
        float currentGround = world.groundHeight(x, z);
        float newGround = world.groundHeight(newX, newZ);
        float heightDiff = newGround - currentGround;
        return !(heightDiff > 1.0f && vy <= 0);
    }
    
    public void jump(float v){if(onGround())vy=v;}

    public void checkAndFixStuckInBlock(World world) {
        if (isPlayerStuckInBlock(world)) {
            attemptToFreePlayer(world);
        }
    }
    
    private boolean isPlayerStuckInBlock(World world) {
    return collidesAt(x, y, z, world);
    }
    
    private void attemptToFreePlayer(World world) {
        if (tryVerticalMovement(world)) {
            return;
        }
        
        if (tryHorizontalMovement(world)) {
            return;
        }
        
        // last resort: move to ground level
        moveToGroundLevel(world);
    }
    
    private boolean tryVerticalMovement(World world) {
        float[] offsets = {0.0f, 1.0f, -1.0f, 2.0f, -2.0f, 3.0f};
        for (float yOffset : offsets) {
            if (canMoveToY(yOffset, world)) {
                y += yOffset;
                return true;
            }
        }
        return false;
    }

    private boolean canMoveToY(float yOffset, World world) {
        float testY = y + yOffset;
        return testY >= 0 && !collidesAt(x, testY, z, world);
    }
    
    private boolean tryHorizontalMovement(World world) {
        float[] offsets = {0.0f, 1.0f, -1.0f, 2.0f, -2.0f, 3.0f};
        for (float xOffset : offsets) {
            for (float zOffset : offsets) {
                if (xOffset == 0 && zOffset == 0) continue;
                if (canMoveToXZ(xOffset, zOffset, world)) {
                    x += xOffset;
                    z += zOffset;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canMoveToXZ(float xOffset, float zOffset, World world) {
        float testX = x + xOffset;
        float testZ = z + zOffset;
        return !collidesAt(testX, y, testZ, world);
    }
    
    private void moveToGroundLevel(World world) {
        float groundHeight = world.groundHeight(x, z);
        y = groundHeight + EYE; // y is eye level, so add EYE height above ground
        vy = 0;
    }
    

    // camera
    public void addYaw(float d){yaw+=d;}
    public void addPitch(float d){pitch=Math.clamp(pitch+d,-89,89);}
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

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }
}