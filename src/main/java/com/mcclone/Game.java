/**
 * Game loop, window management and input handling.
 */

package com.mcclone;

import java.awt.image.BufferedImage;
import java.util.Map;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Main game class.
 *
 * <p>Owns the GLFW window, the {@link World}, the {@link Player} and the
 * {@link Hotbar}, and runs the frame loop until the window is closed.</p>
 */
public class Game {

    /** GLFW window handle. */
    private long window;

    /** The voxel world. */
    private final World world = new World();

    /** The player. */
    private final Player player;

    /** Hotbar UI / inventory. */
    private final Hotbar hotbar = new Hotbar();

    /** Time of the last successful break action. */
    private double lastBreakTime = 0;

    /** Time of the last successful place action. */
    private double lastPlaceTime = 0;

    /** Cooldown between block interactions, in seconds. */
    private static final double COOLDOWN = 0.2;

    /** Mouse sensitivity for camera rotation. */
    private static final float MOUSE_SENSITIVITY = 0.15f;

    /** Result of the latest raycast — what block the crosshair is on, or {@code null}. */
    private int[] highlightedBlock;

    /** Previous cursor X; used by the cursor-pos callback to compute deltas. */
    private double lastCursorX;

    /** Previous cursor Y; used by the cursor-pos callback to compute deltas. */
    private double lastCursorY;

    /** Set to true after the first cursor event so we don't apply an initial jump. */
    private boolean cursorInitialised;

    /** Current keyboard state maintained by GLFW key callbacks. */
    private final InputState[] keyStates = new InputState[GLFW_KEY_LAST + 1];

    /** Current mouse-button state maintained by GLFW mouse callbacks. */
    private final InputState[] mouseButtonStates = new InputState[GLFW_MOUSE_BUTTON_LAST + 1];

    /** True after the player clicks into the window and the cursor is captured. */
    private boolean cursorCaptured;

    /** Mouse movement accumulated by GLFW cursor callbacks between frames. */
    private double queuedMouseDX;

    /** Mouse movement accumulated by GLFW cursor callbacks between frames. */
    private double queuedMouseDY;

    /**
     * Create the game and initialise GLFW/OpenGL.
     *
     * @throws IllegalStateException if GLFW initialisation fails
     */
    public Game() {
        // Error callback prints any GLFW failure to stderr instead of swallowing it.
        org.lwjgl.glfw.GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }

        // Request a sane OpenGL profile — defaults can land you on a 2.1 context
        // on macOS where some calls behave oddly.
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);

        window = glfwCreateWindow(800, 600, "Blockgame 3D", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Failed to create GLFW window");
        }

        for (int i = 0; i < keyStates.length; i++) {
            keyStates[i] = new InputState();
        }
        for (int i = 0; i < mouseButtonStates.length; i++) {
            mouseButtonStates[i] = new InputState();
        }

        glfwMakeContextCurrent(window);
        // Disable vsync. On macOS / Metal, glfwSwapBuffers with swap interval 1
        // can block the main loop almost indefinitely when no input arrives,
        // which collapses the frame loop to "advance once per event". That
        // makes WASD effectively useless because PRESS+RELEASE for the same
        // key are processed in a single batch with no game tick between them.
        glfwSwapInterval(0);
        GL.createCapabilities();

        updateViewportAndPerspective();

        glfwSetFramebufferSizeCallback(window, (win, newW, newH) -> {
            glViewport(0, 0, newW, newH);
            setPerspective(newW, newH);
        });

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.55f, 0.8f, 1.0f, 1);

        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> hotbar.scrollSelection((int) -yoffset));

        Map<String, BufferedImage> textures = TextureGenerator.generateAll();
        for (Map.Entry<String, BufferedImage> e : textures.entrySet()) {
            TextureLoader.registerTexture(e.getKey(), e.getValue());
        }
        TextureLoader.enableTextures();

        float spawnX = 0f;
        float spawnZ = -2f;
        float spawnY = world.groundHeight(spawnX, spawnZ) + Player.getEYE();
        player = new Player(spawnX, spawnY, spawnZ);

        glfwSetWindowFocusCallback(window, (win, focused) -> {
            if (!focused) {
                releaseCursor();
                for (InputState state : keyStates) {
                    state.reset();
                }
                for (InputState state : mouseButtonStates) {
                    state.reset();
                }
            }
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key >= 0 && key < keyStates.length) {
                if (action == GLFW_PRESS) {
                    keyStates[key].setPressed(true);
                } else if (action == GLFW_RELEASE) {
                    keyStates[key].setPressed(false);
                }
            }
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_ESCAPE) {
                    if (cursorCaptured) {
                        releaseCursor();
                    } else {
                        glfwSetWindowShouldClose(window, true);
                    }
                }
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button >= 0 && button < mouseButtonStates.length) {
                if (action == GLFW_PRESS) {
                    mouseButtonStates[button].setPressed(true);
                } else if (action == GLFW_RELEASE) {
                    mouseButtonStates[button].setPressed(false);
                }
            }
            if (action == GLFW_PRESS) {
                if (!cursorCaptured) {
                    captureCursor();
                }
            }
        });

        // Keep this callback only to seed the cursor position. Camera look is
        // polled once per frame in handleMouseLook(), which is more reliable
        // with macOS cursor capture.
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (cursorCaptured) {
                if (!cursorInitialised) {
                    lastCursorX = xpos;
                    lastCursorY = ypos;
                    cursorInitialised = true;
                    return;
                }
                queuedMouseDX += xpos - lastCursorX;
                queuedMouseDY += ypos - lastCursorY;
                lastCursorX = xpos;
                lastCursorY = ypos;
                return;
            }
            lastCursorX = xpos;
            lastCursorY = ypos;
            cursorInitialised = true;
        });

        // Show the window LAST so it pops up only after the GL state is sane.
        glfwShowWindow(window);
        updateViewportAndPerspective();
    }

    private void captureCursor() {
        if (cursorCaptured) {
            return;
        }
        cursorCaptured = true;
        cursorInitialised = false;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(window, x, y);
        lastCursorX = x[0];
        lastCursorY = y[0];
        cursorInitialised = true;
    }

    private void releaseCursor() {
        cursorCaptured = false;
        cursorInitialised = false;
        queuedMouseDX = 0;
        queuedMouseDY = 0;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    private void updateViewportAndPerspective() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        int width = Math.max(1, w[0]);
        int height = Math.max(1, h[0]);
        glViewport(0, 0, width, height);
        setPerspective(width, height);
    }

    private void setPerspective(int w, int h) {
        float aspect = (float) w / h;
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        perspective(70f, aspect, 0.1f, 1000f);
        glMatrixMode(GL_MODELVIEW);
    }

    private static void perspective(float fovY, float aspect, float zNear, float zFar) {
        double fH = Math.tan(Math.toRadians(fovY * 0.5)) * zNear;
        double fW = fH * aspect;
        GL11.glFrustum(-fW, fW, -fH, fH, zNear, zFar);
    }

    /** Draw a small white crosshair at the centre of the screen. */
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
        TextureLoader.disableTextures();

        float cx = w[0] / 2.0f;
        float cy = h[0] / 2.0f;
        float s = 10.0f;
        float t = 2.0f;

        glColor3f(1f, 1f, 1f);
        glBegin(GL_QUADS);
        glVertex2f(cx - s, cy - t / 2); glVertex2f(cx + s, cy - t / 2);
        glVertex2f(cx + s, cy + t / 2); glVertex2f(cx - s, cy + t / 2);
        glEnd();
        glBegin(GL_QUADS);
        glVertex2f(cx - t / 2, cy - s); glVertex2f(cx + t / 2, cy - s);
        glVertex2f(cx + t / 2, cy + s); glVertex2f(cx - t / 2, cy + s);
        glEnd();

        TextureLoader.enableTextures();
        glEnable(GL_DEPTH_TEST);
        glColor3f(1f, 1f, 1f);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    /** Run the main loop until the window closes. */
    public void run() {
        double last = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) Math.min(now - last, 0.05);
            last = now;

            handleInput(dt);
            player.tickPhysics(world, dt);
            player.checkAndFixStuckInBlock(world);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glLoadIdentity();
            player.applyCamera();
            world.render();

            if (highlightedBlock != null) {
                int bx = highlightedBlock[0];
                int by = highlightedBlock[1];
                int bz = highlightedBlock[2];
                GL11.glPushMatrix();
                GL11.glTranslatef(bx - World.SIZE / 2f, by, -bz);
                BlockRenderer.renderOutline();
                GL11.glPopMatrix();
            }

            renderCrosshair();

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

    private void handleInput(float dt) {
        updateInputStates();

        float baseSpeed = Player.WALK_SPEED;
        if (isKeyDown(GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW_KEY_RIGHT_CONTROL)) {
            baseSpeed *= Player.SPRINT_MULT;
        }
        if (isKeyDown(GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
            baseSpeed *= Player.SNEAK_MULT;
        }
        float step = baseSpeed * dt;

        handleMouseLook();

        float dx = 0;
        float dz = 0;

        if (isKeyDown(GLFW_KEY_W)) {
            dz += 1;
        }
        if (isKeyDown(GLFW_KEY_S)) {
            dz -= 1;
        }
        if (isKeyDown(GLFW_KEY_A)) {
            dx -= 1;
        }
        if (isKeyDown(GLFW_KEY_D)) {
            dx += 1;
        }

        if (dx != 0 || dz != 0) {
            player.move(dx, dz, step, world);
        }

        if (isKeyDown(GLFW_KEY_SPACE)) {
            player.jump(Player.JUMP_VELOCITY);
        }

        handleHotbarSelection();
        handleBlockInteraction();
    }

    private void updateInputStates() {
        for (InputState state : keyStates) {
            state.update();
        }
        for (InputState state : mouseButtonStates) {
            state.update();
        }
    }

    private boolean isKeyDown(int key) {
        return key >= 0 && key < keyStates.length && keyStates[key].isDown();
    }

    private boolean wasKeyJustPressed(int key) {
        return key >= 0 && key < keyStates.length && keyStates[key].wasJustPressed();
    }

    private boolean isMouseDown(int button) {
        return button >= 0 && button < mouseButtonStates.length && mouseButtonStates[button].isDown();
    }

    private void handleMouseLook() {
        if (!cursorCaptured) {
            queuedMouseDX = 0;
            queuedMouseDY = 0;
            return;
        }

        double dx = queuedMouseDX;
        double dy = queuedMouseDY;
        queuedMouseDX = 0;
        queuedMouseDY = 0;

        if (dx != 0 || dy != 0) {
            player.addYaw((float) dx * MOUSE_SENSITIVITY);
            // Standard FPS look: dragging the mouse down should pitch the
            // camera down (positive pitch in our convention). Don't negate dy.
            player.addPitch((float) dy * MOUSE_SENSITIVITY);
        }
    }

    private void handleHotbarSelection() {
        for (int i = 0; i < 9; i++) {
            if (wasKeyJustPressed(GLFW_KEY_1 + i)) {
                hotbar.selectSlot(i);
            }
        }
    }

    private void handleBlockInteraction() {
        int[] hit = raycast(player.getEyePosition(), player.getViewVector(), 5.0f);
        highlightedBlock = (hit.length == 0) ? null : hit;

        double now = glfwGetTime();

        if (highlightedBlock != null) {
            if (isMouseDown(GLFW_MOUSE_BUTTON_LEFT) && now - lastBreakTime > COOLDOWN) {
                world.setBlock(highlightedBlock[0], highlightedBlock[1], highlightedBlock[2], BlockType.AIR);
                lastBreakTime = now;
            }
            if (isMouseDown(GLFW_MOUSE_BUTTON_RIGHT) && now - lastPlaceTime > COOLDOWN) {
                int[] placePos = getPlacePosition(highlightedBlock);
                if (placePos.length != 0) {
                    world.setBlock(placePos[0], placePos[1], placePos[2], hotbar.getSelectedItem());
                    lastPlaceTime = now;
                }
            }
        }
    }

    private static final int[] NO_PLACE = new int[0];

    private int[] getPlacePosition(int[] blockAndFace) {
        int x = blockAndFace[0];
        int y = blockAndFace[1];
        int z = blockAndFace[2];
        int face = blockAndFace[3];

        switch (face) {
            case 0: return new int[]{x, y, z + 1}; // North
            case 1: return new int[]{x, y, z - 1}; // South
            case 2: return new int[]{x, y + 1, z}; // Up
            case 3: return new int[]{x, y - 1, z}; // Down
            case 4: return new int[]{x + 1, y, z}; // East
            case 5: return new int[]{x - 1, y, z}; // West
            default: return NO_PLACE;
        }
    }

    /**
     * Cast a ray from the player's eyes to find the block they are looking at.
     *
     * <p>Uses a standard DDA (Digital Differential Analyzer) grid traversal.
     * The eye position and view vector are in world space; we convert into the
     * block array's coordinate system (X offset by SIZE/2, Z negated) before
     * stepping so the traversal is just integer block indices.</p>
     *
     * @param eyePos      player's eye position [x, y, z]
     * @param viewVector  normalized view direction vector [x, y, z]
     * @param maxDistance maximum distance to check, in blocks
     * @return array of [blockX, blockY, blockZ, face] or {@code null} if no block is hit
     */
    public int[] raycast(float[] eyePos, float[] viewVector, float maxDistance) {
        // Convert to block-array coordinates.
        float ox = eyePos[0] + World.SIZE / 2f;
        float oy = eyePos[1];
        float oz = -eyePos[2];

        float dx = viewVector[0];
        float dy = viewVector[1];
        float dz = -viewVector[2];

        int bx = (int) Math.floor(ox);
        int by = (int) Math.floor(oy);
        int bz = (int) Math.floor(oz);

        int stepX = Float.compare(dx, 0);
        int stepY = Float.compare(dy, 0);
        int stepZ = Float.compare(dz, 0);

        float tDeltaX = (dx != 0f) ? Math.abs(1f / dx) : Float.POSITIVE_INFINITY;
        float tDeltaY = (dy != 0f) ? Math.abs(1f / dy) : Float.POSITIVE_INFINITY;
        float tDeltaZ = (dz != 0f) ? Math.abs(1f / dz) : Float.POSITIVE_INFINITY;

        float tMaxX = (dx != 0f) ? (((stepX > 0 ? bx + 1 : bx) - ox) / dx) : Float.POSITIVE_INFINITY;
        float tMaxY = (dy != 0f) ? (((stepY > 0 ? by + 1 : by) - oy) / dy) : Float.POSITIVE_INFINITY;
        float tMaxZ = (dz != 0f) ? (((stepZ > 0 ? bz + 1 : bz) - oz) / dz) : Float.POSITIVE_INFINITY;

        int face = -1;
        float t = 0f;

        // Safety cap in addition to the distance check so we can never loop
        // forever even if something pathological happens.
        int maxIterations = (int) Math.ceil(maxDistance * 3) + 8;
        int iterations = 0;

        while (t <= maxDistance && iterations++ < maxIterations) {
            if (world.hasBlock(bx, by, bz)) {
                return new int[]{bx, by, bz, face};
            }

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                t = tMaxX;
                tMaxX += tDeltaX;
                bx += stepX;
                face = (stepX > 0) ? 5 : 4; // We hit the -X face when stepping +X, so place West (-X)
            } else if (tMaxY < tMaxZ) {
                t = tMaxY;
                tMaxY += tDeltaY;
                by += stepY;
                face = (stepY > 0) ? 3 : 2; // stepping up hits bottom face → place below (Down)
            } else {
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                bz += stepZ;
                face = (stepZ > 0) ? 1 : 0; // stepping +bz hits z-1 face → place at z-1 (South)
            }
        }
        return NO_PLACE;
    }

    public static void main(String[] args) {
        // CRITICAL on macOS: any AWT class (BufferedImage, Graphics2D, ImageIO)
        // initialises Apple's AWT NSApplication, which then hijacks the Cocoa
        // event loop and causes glfwPollEvents() to block forever — callbacks
        // still fire from inside the blocked call, so input looks "received"
        // but the main loop never reaches frame 2.
        //
        // Setting headless mode keeps offscreen AWT (which is all we use for
        // texture generation) working while preventing the NSApp hijack.
        System.setProperty("java.awt.headless", "true");
        // Also tell AWT not to install the Cocoa main runloop hook.
        System.setProperty("apple.awt.UIElement", "true");
        new Game().run();
    }
}
