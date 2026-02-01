/**
 * This file represents the main game loop.
 * It handles the game window, input, and rendering.
 * It uses GLFW for window management and OpenGL for rendering.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import org.lwjgl.opengl.GL;

/**
 * Main game class that manages the game loop, window, rendering, and input handling.
 * This class uses GLFW for window management and OpenGL for 3D rendering.
 * It coordinates interactions between the player, world, and UI components.
 */
public class Game {

    /** GLFW window handle */
    private long window;
    
    /** The game world containing all blocks */
    private final World  world  = new World();
    
    /** The player entity with position and camera */
    private final Player player = new Player(0f, 3f + 1.62f, -2f);
    
    /** The hotbar UI for block selection */
    private final Hotbar hotbar = new Hotbar();
    
    /** Timestamp of the last block break action */
    private double lastBreakTime = 0;
    
    /** Timestamp of the last block place action */
    private double lastPlaceTime = 0;
    
    /** Cooldown period in seconds between block interactions */
    private static final double COOLDOWN = 0.2;
    
    /** Mouse sensitivity multiplier for camera rotation */
    private static final float MOUSE_SENSITIVITY = 0.08f;

    /**
     * Constructs a new Game instance and initializes GLFW, OpenGL, and game systems.
     * Creates an 800x600 window, sets up perspective projection, enables depth testing,
     * captures the mouse cursor, and loads textures.
     * 
     * @throws IllegalStateException if GLFW initialization fails
     */
    public Game() {
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }

        window = glfwCreateWindow(800, 600, "Blockgame 3D", NULL, NULL);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        int[] w = new int[1];
        int[]  h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        setPerspective(w[0], h[0]);

        glfwSetFramebufferSizeCallback(window, (win, newW, newH) -> {
            glViewport(0, 0, newW, newH);
            setPerspective(newW, newH);
        });

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.55f, 0.8f, 1.0f, 1);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> hotbar.scrollSelection((int) -yoffset));

        Block.initTextures();
        TextureLoader.enableTextures();
    }

    /**
     * Sets up the perspective projection matrix based on window dimensions.
     * Configures a 70-degree field of view with near and far clipping planes.
     * 
     * @param w the window width in pixels
     * @param h the window height in pixels
     */
    private void setPerspective(int w, int h) {
        float aspect = (float) w / h;
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        perspective(70f, aspect, 0.1f, 1000f);
        glMatrixMode(GL_MODELVIEW);
    }

    /**
     * Applies a perspective projection using glFrustum.
     * This is a helper method to create a perspective projection matrix.
     * 
     * @param fovY the field of view angle in degrees along the Y axis
     * @param aspect the aspect ratio (width/height)
     * @param zNear the distance to the near clipping plane (must be positive)
     * @param zFar the distance to the far clipping plane (must be positive)
     */
    private static void perspective(float fovY, float aspect, float zNear, float zFar) {
        double fH = Math.tan(Math.toRadians(fovY * 0.5)) * zNear;
        double fW = fH * aspect;
        GL11.glFrustum(-fW, fW, -fH, fH, zNear, zFar);
    }

    /**
     * Renders a white crosshair in the center of the screen.
     * Temporarily switches to orthographic projection, disables depth testing,
     * draws the crosshair as two perpendicular rectangles, then restores the previous state.
     */
    private void renderCrosshair() {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        
        glOrtho(0, w[0], h[0], 0, -1, 1);
        
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        
        glDisable(GL_DEPTH_TEST);
        
        float centerX = w[0] / 2.0f;
        float centerY = h[0] / 2.0f;
        
        float crosshairSize = 10.0f;
        float crosshairThickness = 2.0f;
        
        glColor3f(1.0f, 1.0f, 1.0f);
        
        glBegin(GL_QUADS);
        glVertex2f(centerX - crosshairSize, centerY - crosshairThickness/2);
        glVertex2f(centerX + crosshairSize, centerY - crosshairThickness/2);
        glVertex2f(centerX + crosshairSize, centerY + crosshairThickness/2);
        glVertex2f(centerX - crosshairSize, centerY + crosshairThickness/2);
        glEnd();
        
        glBegin(GL_QUADS);
        glVertex2f(centerX - crosshairThickness/2, centerY - crosshairSize);
        glVertex2f(centerX + crosshairThickness/2, centerY - crosshairSize);
        glVertex2f(centerX + crosshairThickness/2, centerY + crosshairSize);
        glVertex2f(centerX - crosshairThickness/2, centerY + crosshairSize);
        glEnd();
        
        glEnable(GL_DEPTH_TEST);
        glColor3f(1.0f, 1.0f, 1.0f);
        
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    /**
     * Executes the main game loop.
     * Continuously processes input, updates physics, renders the world and UI,
     * and swaps buffers until the window is closed. Calculates delta time
     * for frame-rate independent movement.
     */
    public void run() {
        double last = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            handleInput(dt);
            player.tickPhysics(world);
            player.checkAndFixStuckInBlock(world);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glLoadIdentity();
            player.applyCamera();
            world.render();
            
            renderCrosshair();
            
            // render hotbar UI
            int[] w = new int[1];
            int[] h = new int[1];
            glfwGetFramebufferSize(window, w, h);
            hotbar.render(w[0], h[0]);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        
        TextureLoader.cleanup();
        glfwTerminate();
    }

    /**
     * Processes keyboard and mouse input for player movement and interaction.
     * Handles WASD movement, space for jumping, number keys for hotbar selection,
     * mouse movement for camera rotation, and mouse clicks for block breaking/placing.
     * 
     * @param dt delta time in seconds since the last frame
     */
    private void handleInput(float dt) {
        final float speed = 5 * dt;

        float yaw = player.getYaw();
        if (key(GLFW_KEY_W)) {
            player.move((float) Math.sin(Math.toRadians(yaw)) * speed,
                                         (float)-Math.cos(Math.toRadians(yaw)) * speed, world);
        }
        if (key(GLFW_KEY_S)) {
            player.move((float)-Math.sin(Math.toRadians(yaw)) * speed,
                                         (float) Math.cos(Math.toRadians(yaw)) * speed, world);
        }
        if (key(GLFW_KEY_A)) {
            player.move((float)-Math.cos(Math.toRadians(yaw)) * speed,
                                         (float)-Math.sin(Math.toRadians(yaw)) * speed, world);
        }
        if (key(GLFW_KEY_D)) {
            player.move((float) Math.cos(Math.toRadians(yaw)) * speed,
                                         (float) Math.sin(Math.toRadians(yaw)) * speed, world);
        }
        if (key(GLFW_KEY_SPACE)) {
            player.jump(0.18f);
        }

        handleHotbarSelection();

        double[] mx = new double[1];
        double[] my = new double[1];
        glfwGetCursorPos(window, mx, my);
        raycastForBlockInteraction();
        
        // get current window size for proper mouse handling
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        
        float centerX = w[0] / 2.0f;
        float centerY = h[0] / 2.0f;
        
        // calculate mouse movement from center
        float deltaX = (float)(mx[0] - centerX);
        float deltaY = (float)(centerY - my[0]); // Invert Y for proper camera movement
        
        // only apply rotation if there's actual movement (avoids continuous spinning)
        if (Math.abs(deltaX) > 0.1f || Math.abs(deltaY) > 0.1f) {
            player.addYaw(deltaX * MOUSE_SENSITIVITY);
            player.addPitch(deltaY * MOUSE_SENSITIVITY);
        }
        
        // reset cursor to center
        glfwSetCursorPos(window, centerX, centerY);

        if (key(GLFW_KEY_ESCAPE)) {
            glfwSetWindowShouldClose(window, true);
        }
    }

    /**
     * Handles hotbar slot selection using number keys 1-9.
     * Each key corresponds to a hotbar slot index (1 = slot 0, 2 = slot 1, etc.).
     */
    private void handleHotbarSelection() {
        if (key(GLFW_KEY_1)) {
            hotbar.selectSlot(0);
        }
        if (key(GLFW_KEY_2)) {
            hotbar.selectSlot(1);
        }
        if (key(GLFW_KEY_3)) {
            hotbar.selectSlot(2);
        }
        if (key(GLFW_KEY_4)) {
            hotbar.selectSlot(3);
        }
        if (key(GLFW_KEY_5)) {
            hotbar.selectSlot(4);
        }
        if (key(GLFW_KEY_6)) {
            hotbar.selectSlot(5);
        }
        if (key(GLFW_KEY_7)) {
            hotbar.selectSlot(6);
        }
        if (key(GLFW_KEY_8)) {
            hotbar.selectSlot(7);
        }
        if (key(GLFW_KEY_9)) {
            hotbar.selectSlot(8);
        }
    }

    /**
     * Checks if a specific key is currently pressed.
     * 
     * @param k the GLFW key code to check
     * @return true if the key is pressed, false otherwise
     */
    private boolean key(int k) { return glfwGetKey(window, k) == GLFW_PRESS; }

    /**
     * Performs raycasting from the player's eye position to detect block interactions.
     * Casts a ray in the direction the player is looking and checks for blocks within reach.
     * If a block is found, handles breaking (left click) and placing (Enter key) actions.
     * Uses a step size of 0.05 units along the ray for collision detection.
     */
    private void raycastForBlockInteraction() {
        RaycastData rayData = calculateRaycastData();
        double now = glfwGetTime();
        
        int[] lastAirPos = {-1, -1, -1};
        
        // Use double for loop index to avoid PMD warning about float loop indices
        double stepSize = 0.05;
        for (double t = 0; t <= rayData.reach; t += stepSize) {
            int[] blockPos = getBlockPosition(rayData, (float) t);
            
            if (!world.hasBlock(blockPos[0], blockPos[1], blockPos[2])) {
                updateLastAirPosition(lastAirPos, blockPos);
            } else {
                handleBlockInteraction(blockPos, lastAirPos, now);
                break;
            }
        }
    }
    
    /**
     * Calculates the ray origin and direction for raycasting based on player position and camera angles.
     * The ray starts from the player's eye position and extends in the direction they're looking.
     * 
     * @return a RaycastData object containing ray origin, direction, and reach distance
     */
    private RaycastData calculateRaycastData() {
        float yaw = (float) Math.toRadians(player.getYaw());
        float pitch = (float) Math.toRadians(player.getPitch());

        float ox = player.getX();
        float oy = player.getY() - Player.getEYE() + 1.62f;
        float oz = player.getZ();

        float dx = (float) (Math.sin(yaw) * Math.cos(pitch));
        float dy = (float) Math.sin(pitch);
        float dz = (float) (-Math.cos(yaw) * Math.cos(pitch));
        
        return new RaycastData(ox, oy, oz, dx, dy, dz, 5.0f);
    }
    
    /**
     * Calculates the block coordinates at a specific distance along the raycast.
     * Converts world coordinates to block grid coordinates, accounting for world offset.
     * 
     * @param rayData the raycast data containing origin and direction
     * @param t the distance along the ray from the origin
     * @return an array containing [x, y, z] block coordinates
     */
    private int[] getBlockPosition(RaycastData rayData, float t) {
        float cx = rayData.ox + rayData.dx * t;
        float cy = rayData.oy + rayData.dy * t;
        float cz = rayData.oz + rayData.dz * t;
    
        int bx = (int) Math.floor(cx + World.SIZE / 2f);
        int by = (int) Math.floor(cy);
        int bz = (int) Math.floor(-cz);
        
        return new int[]{bx, by, bz};
    }
    
    /**
     * Updates the last known air block position during raycasting.
     * This is used to determine where to place blocks (in the last empty space before hitting a solid block).
     * 
     * @param lastAirPos the array to update with the last air position [x, y, z]
     * @param blockPos the current block position being checked [x, y, z]
     */
    private void updateLastAirPosition(int[] lastAirPos, int[] blockPos) {
        lastAirPos[0] = blockPos[0];
        lastAirPos[1] = blockPos[1];
        lastAirPos[2] = blockPos[2];
    }
    
    /**
     * Handles both block breaking and placing interactions at the raycast hit location.
     * 
     * @param blockPos the position of the solid block that was hit [x, y, z]
     * @param lastAirPos the last air position before the solid block [x, y, z]
     * @param now the current time in seconds for cooldown checks
     */
    private void handleBlockInteraction(int[] blockPos, int[] lastAirPos, double now) {
        handleBlockBreaking(blockPos, now);
        handleBlockPlacing(lastAirPos, now);
    }
    
    /**
     * Handles block breaking when the left mouse button is pressed.
     * Enforces a cooldown period between consecutive breaks to prevent spam.
     * 
     * @param blockPos the position of the block to break [x, y, z]
     * @param now the current time in seconds
     */
    private void handleBlockBreaking(int[] blockPos, double now) {
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && (now - lastBreakTime) >= COOLDOWN) {
            world.breakBlock(blockPos[0], blockPos[1], blockPos[2]);
            lastBreakTime = now;
        }
    }
    
    /**
     * Handles block placing when the Enter key is pressed.
     * Places the currently selected hotbar item at the last air position before the solid block.
     * Enforces a cooldown period and checks for valid placement conditions.
     * 
     * @param lastAirPos the position where the block should be placed [x, y, z]
     * @param now the current time in seconds
     */
    private void handleBlockPlacing(int[] lastAirPos, double now) {
        if (key(GLFW_KEY_ENTER) && (now - lastPlaceTime) >= COOLDOWN && lastAirPos[0] != -1 && hotbar.hasSelectedItem()) {
            world.placeBlockOfType(lastAirPos[0], lastAirPos[1], lastAirPos[2], hotbar.getSelectedItem());
            lastPlaceTime = now;
        }
    }
    
    /**
     * Data container for raycast calculations.
     * Stores the ray origin, direction vector, and maximum reach distance.
     */
    private static class RaycastData {
        /** Ray origin X coordinate in world space */
        final float ox;
        /** Ray origin Y coordinate in world space */
        final float oy;
        /** Ray origin Z coordinate in world space */
        final float oz;
        /** Ray direction X component (normalized) */
        final float dx;
        /** Ray direction Y component (normalized) */
        final float dy;
        /** Ray direction Z component (normalized) */
        final float dz;
        /** Maximum reach distance for raycasting */
        final float reach;
        
        /**
         * Constructs a new RaycastData with the specified origin, direction, and reach.
         * 
         * @param ox ray origin X coordinate
         * @param oy ray origin Y coordinate
         * @param oz ray origin Z coordinate
         * @param dx ray direction X component
         * @param dy ray direction Y component
         * @param dz ray direction Z component
         * @param reach maximum ray distance
         */
        RaycastData(float ox, float oy, float oz, float dx, float dy, float dz, float reach) {
            this.ox = ox; this.oy = oy; this.oz = oz;
            this.dx = dx; this.dy = dy; this.dz = dz;
            this.reach = reach;
        }
    }

    /**
     * Application entry point. Creates and runs a new Game instance.
     * 
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {
        new Game().run();
    }
}