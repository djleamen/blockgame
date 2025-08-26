/*
 * This file represents the player's hotbar/inventory.
 * It handles the 9-slot hotbar with block selection and rendering.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;

public class Hotbar {
    
    private static final int HOTBAR_SLOTS = 9;
    private int selectedSlot = 0;
    private final int[] items = new int[HOTBAR_SLOTS];
    
    public Hotbar() {
        // initialize hotbar with some blocks
        items[0] = World.BLOCK_TYPE_GRASS;
        items[1] = World.BLOCK_TYPE_PLACED_DIRT;
        items[2] = World.BLOCK_TYPE_COBBLESTONE;
        // slots 3-8 are empty (AIR)
        for (int i = 3; i < HOTBAR_SLOTS; i++) {
            items[i] = World.BLOCK_TYPE_AIR;
        }
    }
    
    public void selectSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SLOTS) {
            selectedSlot = slot;
        }
    }
    
    public void scrollSelection(int direction) {
        selectedSlot += direction;
        if (selectedSlot < 0) {
            selectedSlot = HOTBAR_SLOTS - 1;
        } else if (selectedSlot >= HOTBAR_SLOTS) {
            selectedSlot = 0;
        }
    }
    
    public int getSelectedItem() {
        return items[selectedSlot];
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public boolean hasSelectedItem() {
        return items[selectedSlot] != World.BLOCK_TYPE_AIR;
    }
    
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
                
                switch (items[i]) {
                    case World.BLOCK_TYPE_GRASS:
                        GL11.glColor3f(0.4f, 0.8f, 0.2f);
                        break;
                    case World.BLOCK_TYPE_DIRT:
                    case World.BLOCK_TYPE_PLACED_DIRT:
                        GL11.glColor3f(0.6f, 0.4f, 0.2f);
                        break;
                    case World.BLOCK_TYPE_COBBLESTONE:
                        GL11.glColor3f(0.4f, 0.4f, 0.4f);
                        break;
                    default:
                        GL11.glColor3f(1.0f, 1.0f, 1.0f);
                        break;
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
