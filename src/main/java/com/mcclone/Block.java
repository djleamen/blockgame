/*
 * This file represents a block in the game.
 * It handles the rendering of the block and its textures.
 * It also initializes the textures used for the block.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;

public class Block {
    
    private static int grassTopTexture;
    private static int dirtTexture;
    private static int grassSideTexture;
    private static int cobblestoneTexture;
    
    public static void initTextures() {
        grassTopTexture = TextureLoader.loadTexture("textures/grass_top.png");
        dirtTexture = TextureLoader.loadTexture("textures/dirt.png");
        grassSideTexture = TextureLoader.loadTexture("textures/grass_side.png");
        cobblestoneTexture = TextureLoader.loadTexture("textures/cobblestone.png");
    }

    public void render() {
        TextureLoader.enableTextures();
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        
        // top (grass)
        TextureLoader.bindTexture(grassTopTexture);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 1, -1);
        GL11.glEnd();

        // bottom (dirt)
        TextureLoader.bindTexture(dirtTexture);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 0, -1);
        GL11.glEnd();

        // sides (grass side texture)
        TextureLoader.bindTexture(grassSideTexture);
        GL11.glBegin(GL11.GL_QUADS);

        // +Z
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);

        // –Z
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);

        // –X
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0,  0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1,  0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(0, 1, -1);

        // +X
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(1, 0,  0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(1, 1,  0);

        GL11.glEnd();
    }

    // The following methods are used to render the top and sides of the block separately.
    public void renderTop() {
        TextureLoader.enableTextures();
        TextureLoader.bindTexture(grassTopTexture);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 1, -1);
        GL11.glEnd();
    }

    public void renderSides() {
        TextureLoader.enableTextures();
        TextureLoader.bindTexture(grassSideTexture);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        
        GL11.glBegin(GL11.GL_QUADS);

        // +Z
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);

        // –Z
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);

        // –X
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0,  0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1,  0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(0, 1, -1);

        // +X
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(1, 0,  0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(1, 1,  0);
        GL11.glEnd();
    }
    
    // Render block as grass (original render method)
    public void renderAsGrass() {
        render(); // Use the existing grass rendering
    }
    
    // Render block as pure dirt
    public void renderAsDirt() {
        TextureLoader.enableTextures();
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        
        // all faces use dirt texture
        TextureLoader.bindTexture(dirtTexture);
        
        // top
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 1, -1);
        GL11.glEnd();

        // bottom
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 0, -1);
        GL11.glEnd();

        // sides
        GL11.glBegin(GL11.GL_QUADS);

        // +Z
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);

        // –Z
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);

        // –X
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0,  0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1,  0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(0, 1, -1);

        // +X
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(1, 0,  0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(1, 1,  0);

        GL11.glEnd();
    }
    
    // render block as cobblestone (using proper cobblestone texture)
    public void renderAsCobblestone() {
        TextureLoader.enableTextures();
        GL11.glColor3f(1.0f, 1.0f, 1.0f); // use normal white color for proper texture display
        
        // all faces use cobblestone texture
        TextureLoader.bindTexture(cobblestoneTexture);
        
        // top
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 1, -1);
        GL11.glEnd();

        // bottom
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 0, -1);
        GL11.glEnd();

        // sides
        GL11.glBegin(GL11.GL_QUADS);

        // +Z
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);

        // –Z
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);

        // –X
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0,  0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1,  0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(0, 1, -1);

        // +X
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(1, 0,  0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(1, 1,  0);

        GL11.glEnd();
    }
}