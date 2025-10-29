/*
 * This file represents the main game loop.
 * It handles the game window, input, and rendering.
 * It uses GLFW for window management and OpenGL for rendering.
 */

package com.mcclone;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_7;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_8;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {

    private long window;
    private final World  world  = new World();
    private final Player player = new Player(0f, 3f + 1.62f, -2f);
    private final Hotbar hotbar = new Hotbar();
    private double lastBreakTime = 0;
    private double lastPlaceTime = 0;
    private static final double COOLDOWN = 0.2;
    private static final float MOUSE_SENSITIVITY = 0.08f;

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

    // set the perspective projection matrix 
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

    // render crosshair in the center of the screen
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

    // main game loop
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

    // handle input events (keyboard and mouse)
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

    // handle hotbar selection with number keys (1-9)
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

    private boolean key(int k) { return glfwGetKey(window, k) == GLFW_PRESS; }

    // raycast to find blocks for interaction (break/place)
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
    }    private int[] getBlockPosition(RaycastData rayData, float t) {
        float cx = rayData.ox + rayData.dx * t;
        float cy = rayData.oy + rayData.dy * t;
        float cz = rayData.oz + rayData.dz * t;
    
        int bx = (int) Math.floor(cx + World.SIZE / 2f);
        int by = (int) Math.floor(cy);
        int bz = (int) Math.floor(-cz);
        
        return new int[]{bx, by, bz};
    }
    
    private void updateLastAirPosition(int[] lastAirPos, int[] blockPos) {
        lastAirPos[0] = blockPos[0];
        lastAirPos[1] = blockPos[1];
        lastAirPos[2] = blockPos[2];
    }
    
    private void handleBlockInteraction(int[] blockPos, int[] lastAirPos, double now) {
        handleBlockBreaking(blockPos, now);
        handleBlockPlacing(lastAirPos, now);
    }
    
    private void handleBlockBreaking(int[] blockPos, double now) {
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && (now - lastBreakTime) >= COOLDOWN) {
            world.breakBlock(blockPos[0], blockPos[1], blockPos[2]);
            lastBreakTime = now;
        }
    }
    
    private void handleBlockPlacing(int[] lastAirPos, double now) {
        if (key(GLFW_KEY_ENTER) && (now - lastPlaceTime) >= COOLDOWN && lastAirPos[0] != -1 && hotbar.hasSelectedItem()) {
            world.placeBlockOfType(lastAirPos[0], lastAirPos[1], lastAirPos[2], hotbar.getSelectedItem());
            lastPlaceTime = now;
        }
    }
    
    private static class RaycastData {
        final float ox;
        final float oy;
        final float oz;
        final float dx;
        final float dy;
        final float dz;
        final float reach;
        
        RaycastData(float ox, float oy, float oz, float dx, float dy, float dz, float reach) {
            this.ox = ox; this.oy = oy; this.oz = oz;
            this.dx = dx; this.dy = dy; this.dz = dz;
            this.reach = reach;
        }
    }

    public static void main(String[] args) {
        new Game().run();
    }
}