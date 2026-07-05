/**
 * Player position, view direction and physics.
 *
 * <p>Physics constants are tuned to roughly match early-Minecraft feel rather
 * than real-world numbers: ~28 m/s² gravity, ~8.4 m/s jump velocity (≈ 1.25
 * blocks high), terminal velocity capped at ~78 m/s, and a small step-up
 * tolerance so the player can climb single-block ledges.</p>
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The player entity. Owns position, view angles and a small physics state.
 */
public class Player {

    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    /** Gravity acceleration in blocks/s². */
    private static final float GRAVITY = 28f;

    /** Terminal vertical speed in blocks/s. */
    private static final float TERMINAL_VELOCITY = 78f;

    /** Player eye height above feet. */
    private static final float EYE = 1.62f;

    /** Default walk speed in blocks/s. */
    public static final float WALK_SPEED = 4.317f;

    /** Sprint multiplier on walk speed. */
    public static final float SPRINT_MULT = 1.3f;

    /** Sneak multiplier on walk speed. */
    public static final float SNEAK_MULT = 0.3f;

    /** Jump initial vertical velocity in blocks/s (~1.25 block jump). */
    public static final float JUMP_VELOCITY = 8.4f;

    /** Player collision box width (X and Z) in blocks. */
    private static final float PLAYER_WIDTH = 0.6f;

    /** Player collision box height in blocks. */
    private static final float PLAYER_HEIGHT = 1.8f;

    /** Small tolerance used to keep flush contacts from registering as overlaps. */
    private static final float COLLISION_EPSILON = 0.001f;

    private float x;
    private float y;
    private float z;
    private float pitch;
    private float yaw;

    /** Vertical velocity in blocks/s. */
    private float vy;

    /** Set when the most recent {@link #tickPhysics(World, float)} ended grounded. */
    private boolean grounded;

    /**
     * Construct a player at the given eye-level position.
     *
     * @param x initial world X
     * @param y initial world Y (eye level)
     * @param z initial world Z
     */
    public Player(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Advance gravity and ground collision by {@code dt} seconds.
     *
     * @param world game world for ground queries
     * @param dt    elapsed time in seconds
     */
    public void tickPhysics(World world, float dt) {
        vy -= GRAVITY * dt;
        if (vy < -TERMINAL_VELOCITY) vy = -TERMINAL_VELOCITY;

        // Minecraft-style vertical collision: sweep the player's full AABB
        // along Y and clip the motion against every solid block it would
        // touch (see mcpk.wiki/wiki/Collisions — Y is resolved first, and
        // "on ground" means "collided while moving down"). The old code only
        // tested the single column under the player's centre, which made you
        // fall off blocks whose edge you were standing on and caused jitter
        // and anti-stuck teleports near ledges and ceilings.
        float dy = vy * dt;
        float allowed = clipVerticalMovement(world, dy);
        y += allowed;

        if (dy < 0 && allowed > dy) {
            // Hit the ground part-way through the fall.
            vy = 0;
            grounded = true;
        } else if (dy > 0 && allowed < dy) {
            // Bonked a ceiling.
            vy = 0;
            grounded = false;
        } else {
            grounded = false;
        }

        // Void rescue: if the player has fallen well below the world, respawn
        // them at the original spawn column (their current x/z may be out of
        // bounds, so we deliberately don't reuse it).
        if (y - EYE < -8f) {
            x = 0f;
            z = -2f;
            float spawnGround = world.groundHeight(x, z);
            y = spawnGround + EYE;
            vy = 0;
            grounded = true;
        }
    }

    /**
     * Clip a vertical displacement {@code dy} against every solid block the
     * player's AABB would sweep through, mirroring vanilla's
     * {@code Box.calculateYOffset}: each candidate block reduces the allowed
     * offset so the box ends flush with the obstruction.
     *
     * @param world the world to collide against
     * @param dy    the desired Y displacement this tick (may be 0)
     * @return the largest-magnitude displacement ≤ {@code |dy|} that does not
     *         intersect any solid block
     */
    private float clipVerticalMovement(World world, float dy) {
        if (dy == 0) return 0;

        float minX = x - PLAYER_WIDTH / 2;
        float maxX = x + PLAYER_WIDTH / 2;
        float minZ = z - PLAYER_WIDTH / 2;
        float maxZ = z + PLAYER_WIDTH / 2;
        float feet = y - EYE;
        float head = feet + PLAYER_HEIGHT;

        int minBlockX = (int) Math.floor(minX + World.SIZE / 2f + COLLISION_EPSILON);
        int maxBlockX = (int) Math.floor(maxX + World.SIZE / 2f - COLLISION_EPSILON);
        int minBlockZ = (int) Math.floor(-maxZ + COLLISION_EPSILON);
        int maxBlockZ = (int) Math.floor(-minZ - COLLISION_EPSILON);

        // Vertical range the sweep can touch.
        float sweptMin = Math.min(feet, feet + dy);
        float sweptMax = Math.max(head, head + dy);
        int minBlockY = Math.max(0, (int) Math.floor(sweptMin - 1));
        int maxBlockY = Math.min(World.HEIGHT - 1, (int) Math.floor(sweptMax + 1));

        float allowed = dy;
        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                for (int by = minBlockY; by <= maxBlockY; by++) {
                    if (!world.isSolid(bx, by, bz)) continue;
                    float blockTop = by + 1f;
                    float blockBottom = by;
                    if (allowed < 0 && head > blockBottom && feet >= blockTop - COLLISION_EPSILON) {
                        // Falling: can't pass below this block's top.
                        allowed = Math.max(allowed, blockTop - feet);
                    } else if (allowed > 0 && feet < blockTop && head <= blockBottom + COLLISION_EPSILON) {
                        // Rising: can't pass above this block's bottom.
                        allowed = Math.min(allowed, blockBottom - head);
                    }
                }
            }
        }
        return allowed;
    }

    /** Backwards-compatible fixed-timestep tick used by older callers/tests. */
    public void tickPhysics(World world) {
        tickPhysics(world, 1f / 60f);
    }

    /** @return true if the player is currently on the ground. */
    public boolean onGround() { return grounded; }

    /**
     * Attempt to move the player by a vector derived from ({@code dx}, {@code dz})
     * scaled by {@code step}, relative to the player's current yaw.
     * Each axis is tested independently so the player slides along walls.
     *
     * @param dx    -1 for left, +1 for right
     * @param dz    -1 for back, +1 for forward
     * @param step  movement distance for this frame
     * @param world for collision checks
     */
    public void move(float dx, float dz, float step, World world) {
        // Camera transform is glRotatef(yaw, 0, 1, 0) then glTranslatef(-x, -y, -z),
        // and camera looks down -Z by default. So at yaw=0, "forward" (the
        // direction the camera looks) is world -Z, and "right" is world +X.
        //
        // forward(yaw) = ( sin(yaw), -cos(yaw))
        // right(yaw)   = ( cos(yaw),  sin(yaw))
        double yawRad = Math.toRadians(yaw);
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);

        // Normalise the input vector first so diagonals aren't faster.
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 0) {
            dx /= len;
            dz /= len;
        }

        float moveX =  dz * sinYaw + dx * cosYaw;
        float moveZ = -dz * cosYaw + dx * sinYaw;

        float finalDX = moveX * step;
        float finalDZ = moveZ * step;

        // Vanilla order: Y is resolved in tickPhysics, then X, then Z. Each
        // axis is clipped independently against the AABB so the player slides
        // along walls instead of sticking to them.
        x += clipHorizontalMovement(world, finalDX, true);
        z += clipHorizontalMovement(world, finalDZ, false);
    }

    /**
     * Clip a horizontal displacement against solid blocks, sweeping the
     * player's AABB along one axis (vanilla's {@code calculateXOffset} /
     * {@code calculateZOffset}). Unlike a boolean "can I stand there" test,
     * clipping lets the player advance right up to the wall instead of
     * stopping a partial step away from it.
     *
     * @param world  world to collide against
     * @param d      desired displacement along the axis
     * @param xAxis  true to move along world X, false for world Z
     * @return the allowed displacement ≤ {@code |d|}
     */
    private float clipHorizontalMovement(World world, float d, boolean xAxis) {
        if (d == 0) return 0;

        float minX = x - PLAYER_WIDTH / 2;
        float maxX = x + PLAYER_WIDTH / 2;
        float minZ = z - PLAYER_WIDTH / 2;
        float maxZ = z + PLAYER_WIDTH / 2;
        float feet = y - EYE;
        float head = feet + PLAYER_HEIGHT;

        float sweptMinX = xAxis ? Math.min(minX, minX + d) : minX;
        float sweptMaxX = xAxis ? Math.max(maxX, maxX + d) : maxX;
        float sweptMinZ = xAxis ? minZ : Math.min(minZ, minZ + d);
        float sweptMaxZ = xAxis ? maxZ : Math.max(maxZ, maxZ + d);

        int minBlockX = (int) Math.floor(sweptMinX + World.SIZE / 2f);
        int maxBlockX = (int) Math.floor(sweptMaxX + World.SIZE / 2f);
        int minBlockZ = (int) Math.floor(-sweptMaxZ);
        int maxBlockZ = (int) Math.floor(-sweptMinZ);
        int minBlockY = Math.max(0, (int) Math.floor(feet + COLLISION_EPSILON));
        int maxBlockY = Math.min(World.HEIGHT - 1, (int) Math.floor(head - COLLISION_EPSILON));

        float allowed = d;
        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                for (int by = minBlockY; by <= maxBlockY; by++) {
                    if (!world.isSolid(bx, by, bz)) continue;
                    // Block extents in world space.
                    float blockMinX = bx - World.SIZE / 2f;
                    float blockMaxX = blockMinX + 1f;
                    float blockMaxZ = -bz;
                    float blockMinZ = blockMaxZ - 1f;

                    if (xAxis) {
                        // Must overlap on Z to block X movement.
                        if (maxZ <= blockMinZ + COLLISION_EPSILON || minZ >= blockMaxZ - COLLISION_EPSILON) continue;
                        if (allowed > 0 && maxX <= blockMinX + COLLISION_EPSILON) {
                            allowed = Math.min(allowed, blockMinX - maxX);
                        } else if (allowed < 0 && minX >= blockMaxX - COLLISION_EPSILON) {
                            allowed = Math.max(allowed, blockMaxX - minX);
                        }
                    } else {
                        // Must overlap on X to block Z movement.
                        if (maxX <= blockMinX + COLLISION_EPSILON || minX >= blockMaxX - COLLISION_EPSILON) continue;
                        if (allowed > 0 && maxZ <= blockMinZ + COLLISION_EPSILON) {
                            allowed = Math.min(allowed, blockMinZ - maxZ);
                        } else if (allowed < 0 && minZ >= blockMaxZ - COLLISION_EPSILON) {
                            allowed = Math.max(allowed, blockMaxZ - minZ);
                        }
                    }
                }
            }
        }
        return allowed;
    }

    private boolean collidesAt(float testX, float testY, float testZ, World world) {
        // Standard Minecraft AABB: 0.6 wide × 1.8 tall, eyes at 1.62.
        final float playerWidth = PLAYER_WIDTH;
        final float playerHeight = PLAYER_HEIGHT;
        final float epsilon = COLLISION_EPSILON;

        float minX = testX - playerWidth / 2;
        float maxX = testX + playerWidth / 2;
        float minZ = testZ - playerWidth / 2;
        float maxZ = testZ + playerWidth / 2;
        float minY = testY - EYE;
        float maxY = minY + playerHeight;

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

    private boolean collidesWithBlock(int bx, int by, int bz, float minY, float maxY, World world) {
        if (world.isSolid(bx, by, bz)) {
            float blockMinY = by;
            float blockMaxY = by + 1.0f;
            if (maxY > blockMinY && minY < blockMaxY) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Collision: block ({},{},{}) vs player Y {}-{}",
                            bx, by, bz, String.format("%.3f", minY), String.format("%.3f", maxY));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Jump if currently grounded.
     *
     * @param v initial upward velocity in blocks/s
     */
    public void jump(float v) {
        if (grounded) {
            vy = v;
            grounded = false;
        }
    }

    /**
     * Unstuck the player if they end the tick clipped inside a block.
     *
     * <p>Tries small horizontal nudges first so that "I'm slightly overlapping
     * a block on my side" never turns into a teleport upward. Only after no
     * horizontal escape exists do we try moving the player up — and even then
     * we step in 0.25-block increments rather than jumping a full block, which
     * is what made stepping off a ledge feel like a hop.</p>
     */
    public void checkAndFixStuckInBlock(World world) {
        if (!collidesAt(x, y, z, world)) return;

        // Pass 1: tiny horizontal nudges (eight directions, half-block max).
        float[] horiz = {0.1f, 0.2f, 0.3f, 0.5f};
        for (float r : horiz) {
            float[][] dirs = {
                    { r,  0}, {-r,  0}, { 0,  r}, { 0, -r},
                    { r,  r}, { r, -r}, {-r,  r}, {-r, -r}
            };
            for (float[] d : dirs) {
                if (!collidesAt(x + d[0], y, z + d[1], world)) {
                    x += d[0];
                    z += d[1];
                    return;
                }
            }
        }

        // Pass 2: push up in small steps, in case we're embedded vertically.
        for (float dy = 0.25f; dy <= 3.0f; dy += 0.25f) {
            if (!collidesAt(x, y + dy, z, world)) {
                y += dy;
                vy = 0;
                return;
            }
        }

        // Last resort: snap to ground.
        y = world.groundHeight(x, z) + EYE;
        vy = 0;
    }

    // ----- camera / view ------------------------------------------------------

    /** Add {@code d} degrees to yaw. */
    public void addYaw(float d) { yaw += d; }

    /** Add {@code d} degrees to pitch, clamped to ±89°. */
    public void addPitch(float d) { pitch = Math.max(-89, Math.min(89, pitch + d)); }

    /** Apply the camera transformation to the current OpenGL matrix. */
    public void applyCamera() {
        GL11.glRotatef(pitch, 1, 0, 0);
        GL11.glRotatef(yaw, 0, 1, 0);
        GL11.glTranslatef(-x, -y, -z);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getPitch() { return pitch; }
    public float getYaw() { return yaw; }

    /**
     * Restore a saved pose (used by {@link WorldSave}).
     *
     * @param x     world X
     * @param y     world Y (eye level)
     * @param z     world Z
     * @param pitch view pitch in degrees
     * @param yaw   view yaw in degrees
     */
    public void setPose(float x, float y, float z, float pitch, float yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = Math.max(-89, Math.min(89, pitch));
        this.yaw = yaw;
        this.vy = 0;
    }

    public float[] getEyePosition() {
        return new float[]{x, y, z};
    }

    public float[] getViewVector() {
        // The camera transform is R_pitch * R_yaw * Translate(-eye). Inverting
        // it shows that the world-space forward unit vector is:
        //   F = ( sin(yaw)*cos(pitch),  -sin(pitch),  -cos(yaw)*cos(pitch) )
        // The previous version had the X sign flipped, which made the raycast
        // pick blocks mirrored along X relative to where the player was actually
        // looking — fine at yaw=0 but increasingly wrong as you turn.
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float xz = (float) Math.cos(pitchRad);
        return new float[]{
                xz * (float) Math.sin(yawRad),
                -(float) Math.sin(pitchRad),
                -xz * (float) Math.cos(yawRad)
        };
    }

    public static float getEYE() {
        return EYE;
    }
}
