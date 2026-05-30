/**
 * Procedural texture generation for the block palette.
 *
 * <p>Textures are produced as 16×16 {@link BufferedImage}s and can either be
 * returned in-memory (used by the game at startup) or written to PNG files in
 * {@code src/main/resources/textures} (used by the standalone {@code main}
 * entry point for asset authoring).</p>
 */

package com.mcclone;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * Utility class that produces every block texture used by the game.
 *
 * <p>Each generator method returns a fresh {@code BufferedImage}; callers
 * decide whether to upload it to OpenGL directly via
 * {@link TextureLoader#registerTexture(String, BufferedImage)} or save it to
 * disk.</p>
 *
 * <p>A fixed-seed {@link Random} keeps the textures deterministic across
 * runs so the game looks the same every launch.</p>
 */
public final class TextureGenerator {

    private static final Logger logger = Logger.getLogger(TextureGenerator.class.getName());

    /** Size of generated textures in pixels. */
    private static final int TEXTURE_SIZE = 16;

    /** Deterministic RNG — same seed every run so textures are stable. */
    private static final Random RANDOM = new Random(0xB10C6A47L);

    private TextureGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Build every texture the game needs and return them keyed by the names
     * referenced from {@link BlockType}.
     *
     * @return an ordered map of texture key → 16×16 RGBA image
     */
    public static Map<String, BufferedImage> generateAll() {
        // Reseed so calls are deterministic regardless of previous use.
        RANDOM.setSeed(0xB10C6A47L);

        Map<String, BufferedImage> out = new LinkedHashMap<>();
        out.put("grass_top", grassTop());
        out.put("grass_side", grassSide());
        out.put("dirt", dirt());
        out.put("cobblestone", cobblestone());
        out.put("stone", stone());
        out.put("sand", sand());
        out.put("bedrock", bedrock());
        out.put("log_oak_top", logOakTop());
        out.put("log_oak_side", logOakSide());
        out.put("planks_oak", planksOak());
        out.put("leaves_oak", leavesOak());
        return out;
    }

    /**
     * Generate all textures and also write them to {@code src/main/resources/textures}.
     * Handy when iterating on art outside the game.
     */
    public static void generateTextures() {
        File texturesDir = new File("src/main/resources/textures");
        if (!texturesDir.exists() && !texturesDir.mkdirs()) {
            logger.warning(() -> "Could not create textures dir " + texturesDir);
        }
        Map<String, BufferedImage> all = generateAll();
        for (Map.Entry<String, BufferedImage> e : all.entrySet()) {
            File f = new File(texturesDir, e.getKey() + ".png");
            try {
                ImageIO.write(e.getValue(), "PNG", f);
            } catch (IOException ex) {
                logger.severe(() -> "Failed to write " + f + ": " + ex.getMessage());
            }
        }
        logger.info(() -> "Wrote " + all.size() + " textures to " + texturesDir);
    }

    // ----- individual textures ------------------------------------------------

    private static BufferedImage grassTop() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(91, 153, 48));
        speckle(g, new Color(76, 127, 40), 0.30);
        speckle(g, new Color(106, 179, 56), 0.10);
        g.dispose();
        return img;
    }

    private static BufferedImage dirt() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(134, 96, 67));
        speckle(g, new Color(101, 72, 50), 0.40);
        speckle(g, new Color(156, 112, 78), 0.20);
        speckle(g, new Color(88, 88, 88), 0.05);
        g.dispose();
        return img;
    }

    private static BufferedImage grassSide() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        // dirt base
        g.setColor(new Color(134, 96, 67));
        g.fillRect(0, 4, TEXTURE_SIZE, TEXTURE_SIZE - 4);
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 4; y < TEXTURE_SIZE; y++) {
                if (RANDOM.nextDouble() < 0.3) {
                    g.setColor(new Color(101, 72, 50));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        // grass cap
        g.setColor(new Color(91, 153, 48));
        g.fillRect(0, 0, TEXTURE_SIZE, 4);
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 0; y < 4; y++) {
                if (RANDOM.nextDouble() < 0.2) {
                    g.setColor(new Color(76, 127, 40));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        g.dispose();
        return img;
    }

    private static BufferedImage cobblestone() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(127, 127, 127));
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                double n = RANDOM.nextDouble();
                if (n < 0.3) { g.setColor(new Color(100, 100, 100)); g.fillRect(x, y, 1, 1); }
                else if (n < 0.6) { g.setColor(new Color(150, 150, 150)); g.fillRect(x, y, 1, 1); }
            }
        }
        // mortar lines
        g.setColor(new Color(70, 70, 70));
        for (int y = 3; y < TEXTURE_SIZE; y += 4) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                if (RANDOM.nextDouble() < 0.8) {
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        for (int x = 2; x < TEXTURE_SIZE; x += 5) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                int ox = x + y / 4 % 2 * 2;
                if (ox < TEXTURE_SIZE && RANDOM.nextDouble() < 0.7) {
                    g.fillRect(ox, y, 1, 1);
                }
            }
        }
        // dark specks
        g.setColor(new Color(60, 60, 60));
        for (int i = 0; i < 20; i++) {
            g.fillRect(RANDOM.nextInt(TEXTURE_SIZE), RANDOM.nextInt(TEXTURE_SIZE), 1, 1);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage stone() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(125, 125, 125));
        speckle(g, new Color(110, 110, 110), 0.45);
        speckle(g, new Color(140, 140, 140), 0.20);
        speckle(g, new Color(95, 95, 95), 0.08);
        g.dispose();
        return img;
    }

    private static BufferedImage sand() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(219, 207, 163));
        speckle(g, new Color(204, 192, 144), 0.40);
        speckle(g, new Color(232, 222, 184), 0.15);
        speckle(g, new Color(176, 162, 116), 0.05);
        g.dispose();
        return img;
    }

    private static BufferedImage bedrock() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(70, 70, 70));
        speckle(g, new Color(50, 50, 50), 0.45);
        speckle(g, new Color(95, 95, 95), 0.20);
        speckle(g, new Color(30, 30, 30), 0.10);
        g.dispose();
        return img;
    }

    private static BufferedImage logOakTop() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(170, 138, 88));
        int cx = TEXTURE_SIZE / 2;
        int cy = TEXTURE_SIZE / 2;
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                int ring = (int) (d * 1.8);
                if ((ring % 3) == 0) {
                    g.setColor(new Color(140, 110, 65));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        speckle(g, new Color(120, 92, 55), 0.08);
        g.dispose();
        return img;
    }

    private static BufferedImage logOakSide() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(95, 70, 40));
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                if ((x % 3) == 0 && RANDOM.nextDouble() < 0.7) {
                    g.setColor(new Color(70, 50, 28));
                    g.fillRect(x, y, 1, 1);
                } else if (RANDOM.nextDouble() < 0.15) {
                    g.setColor(new Color(120, 92, 55));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        g.dispose();
        return img;
    }

    private static BufferedImage planksOak() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(178, 142, 88));
        g.setColor(new Color(130, 100, 60));
        for (int y = 3; y < TEXTURE_SIZE; y += 4) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                g.fillRect(x, y, 1, 1);
            }
        }
        for (int y = 0; y < TEXTURE_SIZE; y += 4) {
            int breakX = (y / 4 % 2 == 0) ? 7 : 11;
            for (int yy = y; yy < y + 4 && yy < TEXTURE_SIZE; yy++) {
                g.fillRect(breakX, yy, 1, 1);
            }
        }
        speckle(g, new Color(160, 122, 70), 0.20);
        speckle(g, new Color(196, 162, 110), 0.10);
        g.dispose();
        return img;
    }

    private static BufferedImage leavesOak() {
        BufferedImage img = newImage();
        Graphics2D g = img.createGraphics();
        fill(g, new Color(55, 110, 38));
        speckle(g, new Color(40, 90, 28), 0.45);
        speckle(g, new Color(75, 135, 52), 0.20);
        speckle(g, new Color(28, 70, 22), 0.10);
        g.dispose();
        return img;
    }

    // ----- helpers ------------------------------------------------------------

    private static BufferedImage newImage() {
        return new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static void fill(Graphics2D g, Color c) {
        g.setColor(c);
        g.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static void speckle(Graphics2D g, Color c, double probability) {
        g.setColor(c);
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                if (RANDOM.nextDouble() < probability) {
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
    }

    /**
     * Standalone entry point for regenerating PNGs on disk.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        generateTextures();
    }
}
