/**
 * This file represents the player's hotbar/inventory.
 * It handles the 9-slot hotbar with block selection and rendering.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;

/**
 * Manages the player's hotbar inventory system.
 * Provides a 9-slot hotbar for block selection with rendering and navigation capabilities.
 */
public class Hotbar {
    
    /** Number of slots in the hotbar */
    private static final int HOTBAR_SLOTS = 9;
    
    /** Currently selected slot index (0-8) */
    private int selectedSlot = 0;
    
    /** Array holding block types for each hotbar slot */
    private final int[] items = new int[HOTBAR_SLOTS];
    
    /**
     * Constructs a new Hotbar with default block types.
     * Initializes the first three slots with grass, dirt, and cobblestone.
     * Remaining slots are empty (air blocks).
     */
    public Hotbar() {
        // initialize hotbar with the default Minecraft-style starter palette
        items[0] = World.BLOCK_TYPE_GRASS;
        items[1] = World.BLOCK_TYPE_PLACED_DIRT;
        items[2] = World.BLOCK_TYPE_COBBLESTONE;
        items[3] = World.BLOCK_TYPE_STONE;
        items[4] = World.BLOCK_TYPE_SAND;
        items[5] = World.BLOCK_TYPE_OAK_LOG;
        items[6] = World.BLOCK_TYPE_OAK_PLANKS;
        items[7] = World.BLOCK_TYPE_OAK_LEAVES;
        items[8] = World.BLOCK_TYPE_AIR;
    }
    
    /**
     * Selects a specific hotbar slot by index.
     * 
     * @param slot the slot index to select (0-8)
     */
    public void selectSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SLOTS) {
            selectedSlot = slot;
        }
    }
    
    /**
     * Scrolls the hotbar selection in the specified direction.
     * Wraps around when reaching the first or last slot.
     * 
     * @param direction positive for scrolling right, negative for scrolling left
     */
    public void scrollSelection(int direction) {
        selectedSlot += direction;
        if (selectedSlot < 0) {
            selectedSlot = HOTBAR_SLOTS - 1;
        } else if (selectedSlot >= HOTBAR_SLOTS) {
            selectedSlot = 0;
        }
    }
    
    /**
     * Gets the block type in the currently selected slot.
     * 
     * @return the block type ID of the selected item
     */
    public BlockType getSelectedItem() {
        return BlockType.fromId(items[selectedSlot]);
    }
    
    /**
     * Gets the index of the currently selected slot.
     * 
     * @return the selected slot index (0-8)
     */
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    /**
     * Checks if the currently selected slot contains a placeable block.
     * 
     * @return true if the selected item is not air, false otherwise
     */
    public boolean hasSelectedItem() {
        return items[selectedSlot] != World.BLOCK_TYPE_AIR;
    }
    
    /**
     * Renders the hotbar UI at the bottom center of the screen.
     * Displays all 9 slots with their contents and highlights the selected slot.
     * Uses orthographic projection and disables depth testing for 2D rendering.
     * 
     * @param windowWidth the width of the game window in pixels
     * @param windowHeight the height of the game window in pixels
     */
    public void render(int windowWidth, int windowHeight) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        // set up orthographic projection for 2D UI rendering
        GL11.glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        // disable depth testing for UI elements
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        // hotbar 
        int slotSize = 40;
        int hotbarWidth = HOTBAR_SLOTS * slotSize;
        int hotbarX = (windowWidth - hotbarWidth) / 2;
        int hotbarY = windowHeight - slotSize - 20;
        
        GL11.glColor3f(0.2f, 0.2f, 0.2f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f((float)hotbarX - 4, (float)hotbarY - 4);
        GL11.glVertex2f(hotbarX + (float)hotbarWidth + 4, (float)hotbarY - 4);
        GL11.glVertex2f(hotbarX + (float)hotbarWidth + 4, hotbarY + (float)slotSize + 4);
        GL11.glVertex2f((float)hotbarX - 4, hotbarY + (float)slotSize + 4);
        GL11.glEnd();
        
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int slotX = hotbarX + i * slotSize;
            
            if (i == selectedSlot) {
                GL11.glColor3f(0.8f, 0.8f, 0.8f);
            } else {
                GL11.glColor3f(0.5f, 0.5f, 0.5f);
            }
            
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(slotX, hotbarY);
            GL11.glVertex2f((float)slotX + slotSize, hotbarY);
            GL11.glVertex2f((float)slotX + slotSize, (float)hotbarY + slotSize);
            GL11.glVertex2f(slotX, (float)hotbarY + slotSize);
            GL11.glEnd();
            
            GL11.glColor3f(0.1f, 0.1f, 0.1f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(slotX, hotbarY);
            GL11.glVertex2f((float)slotX + slotSize, hotbarY);
            GL11.glVertex2f((float)slotX + slotSize, (float)hotbarY + slotSize);
            GL11.glVertex2f(slotX, (float)hotbarY + slotSize);
            GL11.glEnd();
            
            if (items[i] != World.BLOCK_TYPE_AIR) {
                int itemSize = 24;
                int itemX = slotX + (slotSize - itemSize) / 2;
                int itemY = hotbarY + (slotSize - itemSize) / 2;
                
                int id = items[i];
                if (id == World.BLOCK_TYPE_GRASS) {
                    GL11.glColor3f(0.4f, 0.8f, 0.2f);
                } else if (id == World.BLOCK_TYPE_DIRT || id == World.BLOCK_TYPE_PLACED_DIRT) {
                    GL11.glColor3f(0.6f, 0.4f, 0.2f);
                } else if (id == World.BLOCK_TYPE_COBBLESTONE) {
                    GL11.glColor3f(0.4f, 0.4f, 0.4f);
                } else if (id == World.BLOCK_TYPE_STONE) {
                    GL11.glColor3f(0.5f, 0.5f, 0.5f);
                } else if (id == World.BLOCK_TYPE_SAND) {
                    GL11.glColor3f(0.86f, 0.81f, 0.64f);
                } else if (id == World.BLOCK_TYPE_BEDROCK) {
                    GL11.glColor3f(0.27f, 0.27f, 0.27f);
                } else if (id == World.BLOCK_TYPE_OAK_LOG) {
                    GL11.glColor3f(0.37f, 0.27f, 0.16f);
                } else if (id == World.BLOCK_TYPE_OAK_PLANKS) {
                    GL11.glColor3f(0.70f, 0.56f, 0.34f);
                } else if (id == World.BLOCK_TYPE_OAK_LEAVES) {
                    GL11.glColor3f(0.22f, 0.43f, 0.15f);
                } else {
                    GL11.glColor3f(1.0f, 1.0f, 1.0f);
                }
                
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(itemX, itemY);
                GL11.glVertex2f((float)itemX + itemSize, itemY);
                GL11.glVertex2f((float)itemX + itemSize, (float)itemY + itemSize);
                GL11.glVertex2f(itemX, (float)itemY + itemSize);
                GL11.glEnd();
            }
        }
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor3f(1.0f, 1.0f, 1.0f); 
        
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
}
