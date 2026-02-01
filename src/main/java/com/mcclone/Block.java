/**
 * This file represents a block in the game.
 * It handles the rendering of the block and its textures.
 * It also initializes the textures used for the block.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;

/**
 * Represents a block in the game world with various rendering methods.
 * <p>
 * This class manages block textures and provides rendering methods for different
 * block types including grass blocks, dirt blocks, and cobblestone blocks.
 * All rendering is done using immediate mode OpenGL (GL_QUADS).
 * </p>
 * <p>
 * Textures must be initialized by calling {@link #initTextures()} before any
 * rendering operations are performed.
 * </p>
 */
public class Block {
    
    /** OpenGL texture ID for the grass block top face. */
    private static int grassTopTexture;
    
    /** OpenGL texture ID for dirt block faces. */
    private static int dirtTexture;
    
    /** OpenGL texture ID for the grass block side faces. */
    private static int grassSideTexture;
    
    /** OpenGL texture ID for cobblestone block faces. */
    private static int cobblestoneTexture;
    
    /**
     * Initializes all block textures by loading them from the textures directory.
     * <p>
     * This method must be called once during game initialization before any
     * rendering operations are performed. It loads the following textures:
     * <ul>
     *   <li>grass_top.png - Top face of grass blocks</li>
     *   <li>dirt.png - Dirt block texture</li>
     *   <li>grass_side.png - Side faces of grass blocks</li>
     *   <li>cobblestone.png - Cobblestone block texture</li>
     * </ul>
     */
    public static void initTextures() {
        grassTopTexture = TextureLoader.loadTexture("textures/grass_top.png");
        dirtTexture = TextureLoader.loadTexture("textures/dirt.png");
        grassSideTexture = TextureLoader.loadTexture("textures/grass_side.png");
        cobblestoneTexture = TextureLoader.loadTexture("textures/cobblestone.png");
    }

    /**
     * Renders a standard grass block with grass texture on top, dirt on bottom,
     * and grass side texture on all four sides.
     * <p>
     * This is the default rendering method for grass blocks. The block is rendered
     * as a 1x1x1 cube using OpenGL quads with appropriate texture coordinates.
     * </p>
     */
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

    /**
     * Renders only the top face of a grass block.
     * <p>
     * This method renders a single quad representing the top face of a grass block
     * with the grass top texture applied. Useful for selective face rendering to
     * improve performance by not rendering hidden faces.
     * </p>
     */
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

    /**
     * Renders only the four side faces of a grass block.
     * <p>
     * This method renders quads for all four vertical sides of the block
     * (+Z, -Z, -X, +X faces) with the grass side texture applied. Does not
     * render the top or bottom faces. Useful for selective face rendering.
     * </p>
     */
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
    
    /**
     * Renders the block as a grass block.
     * <p>
     * This is an alias for the {@link #render()} method, providing a more
     * descriptive name when explicitly rendering grass blocks. Renders with
     * grass top texture, dirt bottom texture, and grass side textures.
     * </p>
     */
    public void renderAsGrass() {
        render(); // Use the existing grass rendering
    }
    
    /**
     * Renders the block as a pure dirt block.
     * <p>
     * All six faces of the block are rendered with the dirt texture applied.
     * This is used when a grass block has been covered by another block above it
     * and should no longer display as grass.
     * </p>
     */
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
    
    /**
     * Renders the block as a cobblestone block.
     * <p>
     * All six faces of the block are rendered with the cobblestone texture applied.
     * Cobblestone blocks are typically used for the foundation layers beneath the
     * surface grass layer or when placed by the player.
     * </p>
     */
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