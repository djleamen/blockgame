/*
 * This file represents a block in the game.
 * It is a simple class that renders a block with a grass top and dirt sides using OpenGL.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;

public class Block {

    public void render() {
        GL11.glBegin(GL11.GL_QUADS);

        // top (grass)
        GL11.glColor3f(0.30f, 0.70f, 0.30f);
        GL11.glVertex3f(0, 1, 0);
        GL11.glVertex3f(1, 1, 0);
        GL11.glVertex3f(1, 1, -1);
        GL11.glVertex3f(0, 1, -1);

        // bottom (dirt)
        GL11.glColor3f(0.39f, 0.26f, 0.13f);
        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(1, 0, 0);
        GL11.glVertex3f(1, 0, -1);
        GL11.glVertex3f(0, 0, -1);

        // sides (dirt)
        GL11.glColor3f(0.39f, 0.26f, 0.13f);

        // +Z
        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(1, 0, 0);
        GL11.glVertex3f(1, 1, 0);
        GL11.glVertex3f(0, 1, 0);

        // –Z
        GL11.glVertex3f(1, 0, -1);
        GL11.glVertex3f(0, 0, -1);
        GL11.glVertex3f(0, 1, -1);
        GL11.glVertex3f(1, 1, -1);

        // –X
        GL11.glVertex3f(0, 0, -1);
        GL11.glVertex3f(0, 0,  0);
        GL11.glVertex3f(0, 1,  0);
        GL11.glVertex3f(0, 1, -1);

        // +X
        GL11.glVertex3f(1, 0,  0);
        GL11.glVertex3f(1, 0, -1);
        GL11.glVertex3f(1, 1, -1);
        GL11.glVertex3f(1, 1,  0);

        GL11.glEnd();
    }

    // The following methods are used to render the top and sides of the block separately.

    public void renderTop() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(0.30f, 0.70f, 0.30f);
        GL11.glVertex3f(0, 1, 0);
        GL11.glVertex3f(1, 1, 0);
        GL11.glVertex3f(1, 1, -1);
        GL11.glVertex3f(0, 1, -1);
        GL11.glEnd();
    }

    public void renderSides() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(0.24f, 0.55f, 0.24f);

        // +Z
        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(1, 0, 0);
        GL11.glVertex3f(1, 1, 0);
        GL11.glVertex3f(0, 1, 0);

        // –Z
        GL11.glVertex3f(1, 0, -1);
        GL11.glVertex3f(0, 0, -1);
        GL11.glVertex3f(0, 1, -1);
        GL11.glVertex3f(1, 1, -1);

        // –X
        GL11.glVertex3f(0, 0, -1);
        GL11.glVertex3f(0, 0,  0);
        GL11.glVertex3f(0, 1,  0);
        GL11.glVertex3f(0, 1, -1);

        // +X
        GL11.glVertex3f(1, 0,  0);
        GL11.glVertex3f(1, 0, -1);
        GL11.glVertex3f(1, 1, -1);
        GL11.glVertex3f(1, 1,  0);
        GL11.glEnd();
    }
}