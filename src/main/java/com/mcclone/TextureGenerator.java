/*
 * This file generates simple textures programmatically for the blocks.
 * These are basic 16x16 pixel textures that mimic early Minecraft style.
 */

package com.mcclone;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class TextureGenerator {
    
    private static final int TEXTURE_SIZE = 16;
    
    public static void generateTextures() {
        try {
            File texturesDir = new File("src/main/resources/textures");
            texturesDir.mkdirs();
            
            generateGrassTopTexture();
            
            generateDirtTexture();
            
            generateGrassSideTexture();
            
            generateCobblestoneTexture();
            
            System.out.println("Generated block textures successfully!");
        } catch (IOException e) {
            System.err.println("Failed to generate textures: " + e.getMessage());
        }
    }
    
    private static void generateGrassTopTexture() throws IOException {
        BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        
        g.setColor(new Color(91, 153, 48));
        g.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                if (Math.random() < 0.3) {
                    g.setColor(new Color(76, 127, 40));
                    g.fillRect(x, y, 1, 1);
                } else if (Math.random() < 0.1) {
                    g.setColor(new Color(106, 179, 56));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File("src/main/resources/textures/grass_top.png"));
    }
    
    private static void generateDirtTexture() throws IOException {
        BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        
        g.setColor(new Color(134, 96, 67));
        g.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                if (Math.random() < 0.4) {
                    g.setColor(new Color(101, 72, 50));
                    g.fillRect(x, y, 1, 1);
                } else if (Math.random() < 0.2) {
                    g.setColor(new Color(156, 112, 78));
                    g.fillRect(x, y, 1, 1);
                } else if (Math.random() < 0.05) {
                    g.setColor(new Color(88, 88, 88));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File("src/main/resources/textures/dirt.png"));
    }
    
    private static void generateGrassSideTexture() throws IOException {
        BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        
        g.setColor(new Color(134, 96, 67));
        g.fillRect(0, 4, TEXTURE_SIZE, TEXTURE_SIZE - 4);
        
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 4; y < TEXTURE_SIZE; y++) {
                if (Math.random() < 0.3) {
                    g.setColor(new Color(101, 72, 50));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        
        g.setColor(new Color(91, 153, 48));
        g.fillRect(0, 0, TEXTURE_SIZE, 4);
        
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 0; y < 4; y++) {
                if (Math.random() < 0.2) {
                    g.setColor(new Color(76, 127, 40));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File("src/main/resources/textures/grass_side.png"));
    }
    
    private static void generateCobblestoneTexture() throws IOException {
        BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        
        g.setColor(new Color(127, 127, 127));
        g.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                double noise = Math.random();
                
                if (noise < 0.3) {
                    g.setColor(new Color(100, 100, 100));
                    g.fillRect(x, y, 1, 1);
                } else if (noise < 0.6) {
                    g.setColor(new Color(150, 150, 150));
                    g.fillRect(x, y, 1, 1);
                } else if (noise < 0.8) {
                    g.setColor(new Color(127, 127, 127));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        
        g.setColor(new Color(70, 70, 70));
        
        for (int y = 3; y < TEXTURE_SIZE; y += 4) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                if (Math.random() < 0.8) {
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        
        for (int x = 2; x < TEXTURE_SIZE; x += 5) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                int offsetX = x + ((y / 4) % 2) * 2;
                if (offsetX < TEXTURE_SIZE && Math.random() < 0.7) {
                    g.fillRect(offsetX, y, 1, 1);
                }
            }
        }
        
        for (int i = 0; i < 20; i++) {
            int x = (int)(Math.random() * TEXTURE_SIZE);
            int y = (int)(Math.random() * TEXTURE_SIZE);
            g.setColor(new Color(60, 60, 60));
            g.fillRect(x, y, 1, 1);
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File("src/main/resources/textures/cobblestone.png"));
    }
    
    public static void main(String[] args) {
        generateTextures();
    }
}
