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
        player.addYaw  ((float)(mx[0] - 400) * 0.08f);
        player.addPitch((float)(300 - my[0]) * 0.08f);
        glfwSetCursorPos(window, 400, 300);

        if (key(GLFW_KEY_ESCAPE)) glfwSetWindowShouldClose(window, true);
    }

    private boolean key(int k) { return glfwGetKey(window, k) == GLFW_PRESS; }

    public static void main(String[] args) {
        new Game().run();
    }
}