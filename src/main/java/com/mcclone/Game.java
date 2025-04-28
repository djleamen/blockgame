/*
 * This file represents the main game loop.
 * It handles the game window, input, and rendering.
 * It uses GLFW for window management and OpenGL for rendering.
 */

package com.mcclone;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {

    private long window;
    private final World  world  = new World();
    private final Player player = new Player(0f, 1f + 1.62f, -2f);
    private double lastBreakTime = 0;
    private double lastPlaceTime = 0;
    private static final double COOLDOWN = 0.2;

    public Game() {
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");

        window = glfwCreateWindow(800, 600, "Blockgame 3D", NULL, NULL);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        setPerspective(w[0], h[0]);

        glfwSetFramebufferSizeCallback(window, (win, newW, newH) -> {
            glViewport(0, 0, newW, newH);
            setPerspective(newW, newH);
        });

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.55f, 0.8f, 1.0f, 1);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
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

    // main game loop
    public void run() {
        double last = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            handleInput(dt);
            player.tickPhysics(world);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glLoadIdentity();
            player.applyCamera();
            world.render();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        glfwTerminate();
    }

    // handle input events (keyboard and mouse)
    private void handleInput(float dt) {
        final float speed = 5 * dt;

        float yaw = player.getYaw();
        if (key(GLFW_KEY_W)) player.move((float) Math.sin(Math.toRadians(yaw)) * speed,
                                         (float)-Math.cos(Math.toRadians(yaw)) * speed);
        if (key(GLFW_KEY_S)) player.move((float)-Math.sin(Math.toRadians(yaw)) * speed,
                                         (float) Math.cos(Math.toRadians(yaw)) * speed);
        if (key(GLFW_KEY_A)) player.move((float)-Math.cos(Math.toRadians(yaw)) * speed,
                                         (float)-Math.sin(Math.toRadians(yaw)) * speed);
        if (key(GLFW_KEY_D)) player.move((float) Math.cos(Math.toRadians(yaw)) * speed,
                                         (float) Math.sin(Math.toRadians(yaw)) * speed);
        if (key(GLFW_KEY_SPACE)) player.jump(0.18f);

        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);
        raycastForBlockInteraction();
        player.addYaw  ((float)(mx[0] - 400) * 0.08f);
        player.addPitch((float)(300 - my[0]) * 0.08f);
        glfwSetCursorPos(window, 400, 300);

        if (key(GLFW_KEY_ESCAPE)) glfwSetWindowShouldClose(window, true);
    }

    private boolean key(int k) { return glfwGetKey(window, k) == GLFW_PRESS; }

    // raycast to find blocks for interaction (break/place)
    private void raycastForBlockInteraction() {
        float yaw = (float) Math.toRadians(player.getYaw());
        float pitch = (float) Math.toRadians(player.getPitch());
    
        float ox = player.x;
        float oy = player.y - Player.getEYE() + 1.62f;
        float oz = player.z;
    
        float dx = (float) (Math.sin(yaw) * Math.cos(pitch));
        float dy = (float) (Math.sin(pitch));
        float dz = (float) (-Math.cos(yaw) * Math.cos(pitch));
    
        float reach = 5.0f;
        double now = glfwGetTime();
    
        int lastAirX = -1, lastAirY = -1, lastAirZ = -1;
    
        for (float t = 0; t <= reach; t += 0.05f) {
            float cx = ox + dx * t;
            float cy = oy + dy * t;
            float cz = oz + dz * t;
    
            int bx = (int) Math.floor(cx + World.SIZE / 2f);
            int by = (int) Math.floor(cy);
            int bz = (int) Math.floor(-cz);
    
            if (!world.hasBlock(bx, by, bz)) {
                lastAirX = bx;
                lastAirY = by;
                lastAirZ = bz;
                continue;
            } else {
                if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && (now - lastBreakTime) >= COOLDOWN) {
                    world.breakBlock(bx, by, bz);
                    lastBreakTime = now;
                }
                if (key(GLFW_KEY_ENTER) && (now - lastPlaceTime) >= COOLDOWN && lastAirX != -1) {
                    world.placeBlock(lastAirX, lastAirY, lastAirZ);
                    lastPlaceTime = now;
                }
                break;
            }
        }
    }

    public static void main(String[] args) {
        new Game().run();
    }
}