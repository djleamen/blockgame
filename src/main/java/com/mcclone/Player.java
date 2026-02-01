/**
 * This file represents a Player in the game.
 * It handles the player's position, movement, and camera controls.
 * It also handles the player's physics, including gravity and jumping.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a player in the game world.
 * Handles player position, movement, physics (gravity, jumping, collision),
 * and camera controls (pitch and yaw rotation).
 */
public class Player {

    private static final Logger logger = LoggerFactory.getLogger(Player.class);
    
    /** Gravity acceleration constant applied each physics tick */
    private static final float GRAVITY = 0.008f;
    
    /** Player eye height above feet position */
    private static final float EYE = 1.62f;

    /** Player X position in world space */
    private float x;
    
    /** Player Y position in world space (eye level) */
    private float y;
    
    /** Player Z position in world space */
    private float z;
    
    /** Camera pitch angle in degrees (-89 to 89) */
    private float pitch;
    
    /** Camera yaw angle in degrees */
    private float yaw;
    
    /** Vertical velocity for gravity and jumping */
    private float vy;

    /**
     * Constructs a new Player at the specified position.
     * 
     * @param x the initial X coordinate (world space)
     * @param y the initial Y coordinate (eye level)
     * @param z the initial Z coordinate (world space)
     */
    public Player(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Updates player physics including gravity and ground collision.
     * Called each frame to apply gravity and check if player has landed on ground.
     * 
     * @param world the game world for ground height calculations
     */
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
    
    /**
     * Checks if the player is currently on the ground.
     * 
     * @return true if vertical velocity is zero (grounded), false otherwise
     */
    public boolean onGround(){return vy==0;}
    
    /**
     * Attempts to move the player by the specified horizontal offset.
     * Checks for collisions and prevents movement into solid blocks.
     * 
     * @param dx the change in X position
     * @param dz the change in Z position
     * @param world the game world for collision checking
     */
    public void move(float dx, float dz, World world) {
        float newX = x + dx;
        float newZ = z + dz;

        // check collision at new position
        if (canMoveTo(newX, newZ, world)) {
            x = newX;
            z = newZ;
        }
    }
    
    /**
     * Checks if the player can move to the specified position.
     * 
     * @param newX the target X position
     * @param newZ the target Z position
     * @param world the game world for collision checking
     * @return true if movement is valid, false if blocked
     */
    private boolean canMoveTo(float newX, float newZ, World world) {
        return !collidesAt(newX, y, newZ, world) && canClimbTo(newX, newZ, world);
    }

    /**
     * Checks if the player's bounding box collides with any blocks at the specified position.
     * Uses a player width of 0.6 blocks and height of 2.0 blocks.
     * 
     * @param testX the X position to test
     * @param testY the Y position to test (eye level)
     * @param testZ the Z position to test
     * @param world the game world for block checking
     * @return true if collision detected, false otherwise
     */
    private boolean collidesAt(float testX, float testY, float testZ, World world) {
        final float playerWidth = 0.6f;
        final float playerHeight = 2.0f;
        final float collisionBuffer = 0.1f;
        final float epsilon = 0.001f;

        float minX = testX - playerWidth/2 - collisionBuffer;
        float maxX = testX + playerWidth/2 + collisionBuffer;
        float minZ = testZ - playerWidth/2 - collisionBuffer;
        float maxZ = testZ + playerWidth/2 + collisionBuffer;
        float minY = testY - EYE;
        float maxY = testY + (playerHeight - EYE);

        int minBlockX = (int) Math.floor(minX + World.SIZE / 2f);
        int maxBlockX = (int) Math.floor(maxX + World.SIZE / 2f);
        int minBlockZ = (int) Math.floor(-maxZ);
        int maxBlockZ = (int) Math.floor(-minZ);
        int minBlockY = (int) Math.floor(minY + epsilon);
        int maxBlockY = (int) Math.floor(maxY - epsilon);

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

    /**
     * Checks if a specific block collides with the player's Y range.
     * Logs debug information when collision is detected.
     * 
     * @param bx the block X coordinate
     * @param by the block Y coordinate
     * @param bz the block Z coordinate
     * @param minY the player's minimum Y position (feet)
     * @param maxY the player's maximum Y position (head)
     * @param world the game world for block checking
     * @return true if collision detected, false otherwise
     */
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

    /**
     * Checks if the player can climb to the specified position.
     * Prevents climbing more than 1 block height without jumping.
     * 
     * @param newX the target X position
     * @param newZ the target Z position
     * @param world the game world for ground height calculations
     * @return true if climbable, false if too steep
     */
    private boolean canClimbTo(float newX, float newZ, World world) {
        float currentGround = world.groundHeight(x, z);
        float newGround = world.groundHeight(newX, newZ);
        float heightDiff = newGround - currentGround;
        return !(heightDiff > 1.0f && vy <= 0);
    }
    
    /**
     * Makes the player jump with the specified initial velocity.
     * Only works if the player is currently on the ground.
     * 
     * @param v the initial upward velocity for the jump
     */
    public void jump(float v) {
        if (onGround()) {
            vy = v;
        }
    }

    /**
     * Checks if the player is stuck inside a block and attempts to free them.
     * This is an anti-stuck system to prevent players from getting trapped.
     * 
     * @param world the world to check for block collisions
     */
    public void checkAndFixStuckInBlock(World world) {
        if (isPlayerStuckInBlock(world)) {
            attemptToFreePlayer(world);
        }
    }
    
    /**
     * Checks if the player is currently colliding with blocks.
     * 
     * @param world the game world for collision checking
     * @return true if player is stuck in a block, false otherwise
     */
    private boolean isPlayerStuckInBlock(World world) {
    return collidesAt(x, y, z, world);
    }
    
    /**
     * Attempts to free a stuck player by trying vertical then horizontal movement.
     * Falls back to moving to ground level if other methods fail.
     * 
     * @param world the game world for collision checking
     */
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
    
    /**
     * Attempts to free the player by moving vertically.
     * Tries offsets of 0, ±1, ±2, and 3 blocks.
     * 
     * @param world the game world for collision checking
     * @return true if successfully freed, false otherwise
     */
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

    /**
     * Checks if the player can move to a Y position offset.
     * 
     * @param yOffset the Y offset to test
     * @param world the game world for collision checking
     * @return true if the position is valid and collision-free
     */
    private boolean canMoveToY(float yOffset, World world) {
        float testY = y + yOffset;
        return testY >= 0 && !collidesAt(x, testY, z, world);
    }
    
    /**
     * Attempts to free the player by moving horizontally.
     * Tries various X and Z offset combinations.
     * 
     * @param world the game world for collision checking
     * @return true if successfully freed, false otherwise
     */
    private boolean tryHorizontalMovement(World world) {
        float[] offsets = {0.0f, 1.0f, -1.0f, 2.0f, -2.0f, 3.0f};
        for (float xOffset : offsets) {
            for (float zOffset : offsets) {
                if (xOffset == 0 && zOffset == 0) {
                    continue;
                }
                if (canMoveToXZ(xOffset, zOffset, world)) {
                    x += xOffset;
                    z += zOffset;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the player can move to a horizontal position offset.
     * 
     * @param xOffset the X offset to test
     * @param zOffset the Z offset to test
     * @param world the game world for collision checking
     * @return true if the position is collision-free
     */
    private boolean canMoveToXZ(float xOffset, float zOffset, World world) {
        float testX = x + xOffset;
        float testZ = z + zOffset;
        return !collidesAt(testX, y, testZ, world);
    }
    
    /**
     * Moves the player to ground level at their current horizontal position.
     * Used as a last resort when other unstuck methods fail.
     * 
     * @param world the game world for ground height calculations
     */
    private void moveToGroundLevel(World world) {
        float groundHeight = world.groundHeight(x, z);
        y = groundHeight + EYE; // y is eye level, so add EYE height above ground
        vy = 0;
    }
    

    /**
     * Adds a delta to the camera yaw angle.
     * 
     * @param d the yaw change in degrees
     */
    public void addYaw(float d){yaw+=d;}
    
    /**
     * Adds a delta to the camera pitch angle.
     * Clamps pitch to prevent looking beyond vertical limits (-89 to 89 degrees).
     * 
     * @param d the pitch change in degrees
     */
    public void addPitch(float d){pitch=Math.max(-89, Math.min(89, pitch+d));}
    
    /**
     * Gets the current camera yaw angle.
     * 
     * @return the yaw angle in degrees
     */
    public float getYaw(){return yaw;}
    
    /**
     * Gets the current camera pitch angle.
     * 
     * @return the pitch angle in degrees
     */
    public float getPitch(){return pitch;}

    /**
     * Applies the camera transformation to the current OpenGL matrix.
     * Rotates by pitch and yaw, then translates to player position.
     */
    public void applyCamera(){
        GL11.glRotatef(-pitch,1,0,0);
        GL11.glRotatef( yaw ,0,1,0);
        GL11.glTranslatef(-x,-y,-z);
    }

    /**
     * Gets the player eye height constant.
     * 
     * @return the eye height above feet position
     */
    public static float getEYE() {
        return EYE;
    }

    /**
     * Gets the player's X position.
     * 
     * @return the X coordinate in world space
     */
    public float getX() {
        return x;
    }

    /**
     * Gets the player's Y position (eye level).
     * 
     * @return the Y coordinate in world space
     */
    public float getY() {
        return y;
    }

    /**
     * Gets the player's Z position.
     * 
     * @return the Z coordinate in world space
     */
    public float getZ() {
        return z;
    }

    /**
     * Sets the player's X position.
     * 
     * @param x the new X coordinate
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * Sets the player's Y position.
     * 
     * @param y the new Y coordinate (eye level)
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * Sets the player's Z position.
     * 
     * @param z the new Z coordinate
     */
    public void setZ(float z) {
        this.z = z;
    }
}