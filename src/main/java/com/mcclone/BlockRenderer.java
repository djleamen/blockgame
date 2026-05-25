/**
 * Per-face block renderer with neighbour-based face culling.
 *
 * <p>Replaces the older {@code Block} renderer which always emitted all six
 * faces of every block. Each face is drawn only when the neighbouring block
 * on that side is non-opaque (air, out of bounds, or a future translucent
 * type), which is the simplest meaningful optimisation while we are still on
 * immediate-mode OpenGL.</p>
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;

/**
 * Stateless helper that renders a single block at the origin in local model
 * space. Callers are responsible for the {@code glTranslatef} that places the
 * block in the world.
 *
 * <p>The block occupies the unit cube {@code [0,1] × [0,1] × [-1,0]} to match
 * the original {@code Block} implementation and the world's
 * {@code (x - SIZE/2, y, -z)} translation in {@link World#render()}.</p>
 */
public final class BlockRenderer {

    private BlockRenderer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Render the visible faces of {@code type} at world coords ({@code bx},
     * {@code by}, {@code bz}). The caller has already applied the
     * {@code glTranslatef} for this block.
     *
     * @param type  the block kind to draw
     * @param bx    block x in the world array
     * @param by    block y in the world array
     * @param bz    block z in the world array
     * @param world world used for neighbour lookups
     */
    public static void render(BlockType type, int bx, int by, int bz, World world) {
        if (type == BlockType.AIR) {
            return;
        }
        TextureLoader.enableTextures();
        GL11.glColor3f(1.0f, 1.0f, 1.0f);

        boolean drawTop    = !world.isOpaque(bx, by + 1, bz);
        boolean drawBottom = !world.isOpaque(bx, by - 1, bz);
        boolean drawPosZ   = !world.isOpaque(bx, by, bz - 1); // +Z in model space ⇒ -z block
        boolean drawNegZ   = !world.isOpaque(bx, by, bz + 1);
        boolean drawNegX   = !world.isOpaque(bx - 1, by, bz);
        boolean drawPosX   = !world.isOpaque(bx + 1, by, bz);

        if (drawTop)    drawTop(type);
        if (drawBottom) drawBottom(type);
        if (drawPosZ)   drawPosZ(type);
        if (drawNegZ)   drawNegZ(type);
        if (drawNegX)   drawNegX(type);
        if (drawPosX)   drawPosX(type);
    }

    /**
     * Convenience: render all six faces unconditionally. Used for picking
     * highlights and for tests where neighbour info is not available.
     *
     * @param type the block kind to draw
     */
    public static void renderUnculled(BlockType type) {
        if (type == BlockType.AIR) {
            return;
        }
        TextureLoader.enableTextures();
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        drawTop(type);
        drawBottom(type);
        drawPosZ(type);
        drawNegZ(type);
        drawNegX(type);
        drawPosX(type);
    }

    // ----- per-face helpers ---------------------------------------------------

    private static void bind(String key) {
        TextureLoader.bindTexture(TextureLoader.getTexture(key));
    }

    private static void drawTop(BlockType type) {
        bind(type.topTexture());
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 1, -1);
        GL11.glEnd();
    }

    private static void drawBottom(BlockType type) {
        bind(type.bottomTexture());
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 0, -1);
        GL11.glEnd();
    }

    private static void drawPosZ(BlockType type) {
        bind(type.sideTexture());
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, 0);
        GL11.glEnd();
    }

    private static void drawNegZ(BlockType type) {
        bind(type.sideTexture());
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);
        GL11.glEnd();
    }

    private static void drawNegX(BlockType type) {
        bind(type.sideTexture());
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(0, 0, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0, 0,  0);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0, 1,  0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(0, 1, -1);
        GL11.glEnd();
    }

    private static void drawPosX(BlockType type) {
        bind(type.sideTexture());
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(1, 0,  0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 0, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(1, 1,  0);
        GL11.glEnd();
    }

    /**
     * Draw a wireframe outline around the block at the origin.
     * Used by the targeted-block highlight.
     */
    public static void renderOutline() {
        boolean wasLightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        if (wasLightingEnabled) GL11.glDisable(GL11.GL_LIGHTING);
        TextureLoader.disableTextures();
        GL11.glColor4f(0f, 0f, 0f, 1f);
        GL11.glLineWidth(2.0f);
        float p = -0.001f;
        float q = 1.001f;
        float zp = 0.001f;
        float zq = -1.001f;

        // top
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3f(p, q, zp); GL11.glVertex3f(q, q, zp);
        GL11.glVertex3f(q, q, zq); GL11.glVertex3f(p, q, zq);
        GL11.glEnd();
        // bottom
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3f(p, p, zp); GL11.glVertex3f(q, p, zp);
        GL11.glVertex3f(q, p, zq); GL11.glVertex3f(p, p, zq);
        GL11.glEnd();
        // verticals
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3f(p, p, zp); GL11.glVertex3f(p, q, zp);
        GL11.glVertex3f(q, p, zp); GL11.glVertex3f(q, q, zp);
        GL11.glVertex3f(q, p, zq); GL11.glVertex3f(q, q, zq);
        GL11.glVertex3f(p, p, zq); GL11.glVertex3f(p, q, zq);
        GL11.glEnd();

        GL11.glColor4f(1f, 1f, 1f, 1f);
        TextureLoader.enableTextures();
        if (wasLightingEnabled) GL11.glEnable(GL11.GL_LIGHTING);
    }
}
