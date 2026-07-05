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

    /** Number of storage slots in the inventory screen (3 rows of 9). */
    private static final int STORAGE_SLOTS = 27;

    /** Maximum number of blocks a single slot can hold. */
    private static final int MAX_STACK = 64;

    /** Slot pixel size shared by the hotbar and the inventory screen. */
    private static final int SLOT_SIZE = 40;
    
    /** Currently selected slot index (0-8) */
    private int selectedSlot = 0;
    
    /** Array holding block types for each hotbar slot */
    private final int[] items = new int[HOTBAR_SLOTS];

    /** Number of blocks held in each slot (0 when the slot is empty). */
    private final int[] counts = new int[HOTBAR_SLOTS];

    /** Block ids for the 27 inventory storage slots. */
    private final int[] storageItems = new int[STORAGE_SLOTS];

    /** Counts for the 27 inventory storage slots. */
    private final int[] storageCounts = new int[STORAGE_SLOTS];

    /** Block id currently picked up by the mouse inside the inventory screen. */
    private int heldId = 0;

    /** Count of the stack currently picked up by the mouse (0 = nothing held). */
    private int heldCount = 0;
    
    /**
     * Constructs a new, empty Hotbar.
     * Blocks are collected by breaking them in the world (survival-style)
     * and stack up to {@link #MAX_STACK} per slot. When the hotbar fills up,
     * further pickups overflow into the 27-slot storage (press E to open).
     */
    public Hotbar() {
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            items[i] = World.BLOCK_TYPE_AIR;
            counts[i] = 0;
        }
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            storageItems[i] = World.BLOCK_TYPE_AIR;
            storageCounts[i] = 0;
        }
    }

    /**
     * Add one block of the given type: stacks onto the hotbar first, then an
     * empty hotbar slot, then the inventory storage.
     *
     * @param type block type to collect
     * @return true if the block was stored, false if everything is full
     */
    public boolean addItem(BlockType type) {
        if (type == null || type == BlockType.AIR) {
            return false;
        }
        int id = type.id();
        if (addToSlots(items, counts, id)) {
            return true;
        }
        return addToSlots(storageItems, storageCounts, id);
    }

    /** Stack one block into the given slot arrays; existing stacks first. */
    private static boolean addToSlots(int[] slotItems, int[] slotCounts, int id) {
        for (int i = 0; i < slotItems.length; i++) {
            if (slotItems[i] == id && slotCounts[i] > 0 && slotCounts[i] < MAX_STACK) {
                slotCounts[i]++;
                return true;
            }
        }
        for (int i = 0; i < slotItems.length; i++) {
            if (slotCounts[i] == 0) {
                slotItems[i] = id;
                slotCounts[i] = 1;
                return true;
            }
        }
        return false;
    }

    /**
     * Remove one block from the selected slot (called after a successful
     * placement). Empties the slot when the count reaches zero.
     */
    public void consumeSelected() {
        if (counts[selectedSlot] > 0) {
            counts[selectedSlot]--;
            if (counts[selectedSlot] == 0) {
                items[selectedSlot] = World.BLOCK_TYPE_AIR;
            }
        }
    }

    /**
     * @param slot slot index (0-8)
     * @return the number of blocks currently held in the slot
     */
    public int getCount(int slot) {
        return (slot >= 0 && slot < HOTBAR_SLOTS) ? counts[slot] : 0;
    }

    /**
     * @param slot slot index (0-8)
     * @return the raw block id stored in the slot (AIR id when empty)
     */
    public int getItemId(int slot) {
        return (slot >= 0 && slot < HOTBAR_SLOTS) ? items[slot] : World.BLOCK_TYPE_AIR;
    }

    /**
     * Overwrite a slot's contents (used when restoring a save).
     *
     * @param slot  slot index (0-8)
     * @param id    block id
     * @param count block count; 0 empties the slot
     */
    public void setSlot(int slot, int id, int count) {
        if (slot < 0 || slot >= HOTBAR_SLOTS) return;
        if (count <= 0 || BlockType.fromId(id) == BlockType.AIR) {
            items[slot] = World.BLOCK_TYPE_AIR;
            counts[slot] = 0;
        } else {
            items[slot] = BlockType.fromId(id).id();
            counts[slot] = Math.min(count, MAX_STACK);
        }
    }

    /** @return the number of hotbar slots. */
    public static int slotCount() {
        return HOTBAR_SLOTS;
    }

    /** @return the number of inventory storage slots. */
    public static int storageSlotCount() {
        return STORAGE_SLOTS;
    }

    /**
     * @param slot storage slot index (0-26)
     * @return the raw block id stored in the storage slot (AIR id when empty)
     */
    public int getStorageId(int slot) {
        return (slot >= 0 && slot < STORAGE_SLOTS) ? storageItems[slot] : World.BLOCK_TYPE_AIR;
    }

    /**
     * @param slot storage slot index (0-26)
     * @return the number of blocks currently held in the storage slot
     */
    public int getStorageCount(int slot) {
        return (slot >= 0 && slot < STORAGE_SLOTS) ? storageCounts[slot] : 0;
    }

    /**
     * Overwrite a storage slot's contents (used when restoring a save).
     *
     * @param slot  storage slot index (0-26)
     * @param id    block id
     * @param count block count; 0 empties the slot
     */
    public void setStorageSlot(int slot, int id, int count) {
        if (slot < 0 || slot >= STORAGE_SLOTS) return;
        if (count <= 0 || BlockType.fromId(id) == BlockType.AIR) {
            storageItems[slot] = World.BLOCK_TYPE_AIR;
            storageCounts[slot] = 0;
        } else {
            storageItems[slot] = BlockType.fromId(id).id();
            storageCounts[slot] = Math.min(count, MAX_STACK);
        }
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
        return items[selectedSlot] != World.BLOCK_TYPE_AIR && counts[selectedSlot] > 0;
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
        beginUi(windowWidth, windowHeight);
        drawHotbarRow(windowWidth, windowHeight);
        endUi();
    }

    /** Draw the 9-slot hotbar row at the bottom of the screen. */
    private void drawHotbarRow(int windowWidth, int windowHeight) {
        int hotbarWidth = HOTBAR_SLOTS * SLOT_SIZE;
        int hotbarX = (windowWidth - hotbarWidth) / 2;
        int hotbarY = windowHeight - SLOT_SIZE - 20;

        GL11.glColor3f(0.2f, 0.2f, 0.2f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f((float)hotbarX - 4, (float)hotbarY - 4);
        GL11.glVertex2f(hotbarX + (float)hotbarWidth + 4, (float)hotbarY - 4);
        GL11.glVertex2f(hotbarX + (float)hotbarWidth + 4, hotbarY + (float)SLOT_SIZE + 4);
        GL11.glVertex2f((float)hotbarX - 4, hotbarY + (float)SLOT_SIZE + 4);
        GL11.glEnd();

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            drawSlot(hotbarX + i * SLOT_SIZE, hotbarY, items[i], counts[i], i == selectedSlot);
        }
    }

    /**
     * Render the inventory screen: dimmed backdrop, 3×9 storage grid in the
     * middle of the screen, and the stack currently held by the mouse (if any)
     * following the cursor. The regular hotbar at the bottom stays visible and
     * clickable while the screen is open.
     *
     * @param windowWidth  framebuffer width in pixels
     * @param windowHeight framebuffer height in pixels
     * @param mouseX       cursor X in framebuffer pixels
     * @param mouseY       cursor Y in framebuffer pixels
     */
    public void renderInventoryScreen(int windowWidth, int windowHeight, float mouseX, float mouseY) {
        beginUi(windowWidth, windowHeight);

        // Dim the world behind the screen.
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(0f, 0f, 0f, 0.55f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(windowWidth, 0);
        GL11.glVertex2f(windowWidth, windowHeight);
        GL11.glVertex2f(0, windowHeight);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_BLEND);

        // Hotbar drawn above the dim so it reads as part of the screen.
        drawHotbarRow(windowWidth, windowHeight);

        int gridX = storageGridX(windowWidth);
        int gridY = storageGridY(windowHeight);
        int gridW = 9 * SLOT_SIZE;
        int gridH = 3 * SLOT_SIZE;

        // Panel background.
        GL11.glColor3f(0.2f, 0.2f, 0.2f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f((float)gridX - 6, (float)gridY - 6);
        GL11.glVertex2f(gridX + (float)gridW + 6, (float)gridY - 6);
        GL11.glVertex2f(gridX + (float)gridW + 6, gridY + (float)gridH + 6);
        GL11.glVertex2f((float)gridX - 6, gridY + (float)gridH + 6);
        GL11.glEnd();

        for (int i = 0; i < STORAGE_SLOTS; i++) {
            int sx = gridX + (i % 9) * SLOT_SIZE;
            int sy = gridY + (i / 9) * SLOT_SIZE;
            drawSlot(sx, sy, storageItems[i], storageCounts[i], false);
        }

        // Held stack follows the cursor, drawn last so it sits on top.
        if (heldCount > 0) {
            drawItemSwatch((int) mouseX - 12, (int) mouseY - 12, heldId);
            if (heldCount > 1) {
                GL11.glColor3f(1f, 1f, 1f);
                drawNumber(heldCount, (int) mouseX + 14, (int) mouseY + 2);
            }
        }

        endUi();
    }

    /**
     * Handle a left click inside the inventory screen, Minecraft-style:
     * pick up a stack, drop the held stack into an empty slot, merge onto a
     * stack of the same type (leftover stays held), or swap different types.
     *
     * @param mouseX       cursor X in framebuffer pixels
     * @param mouseY       cursor Y in framebuffer pixels
     * @param windowWidth  framebuffer width in pixels
     * @param windowHeight framebuffer height in pixels
     */
    public void handleInventoryClick(float mouseX, float mouseY, int windowWidth, int windowHeight) {
        int gridX = storageGridX(windowWidth);
        int gridY = storageGridY(windowHeight);
        int col = (int) Math.floor((mouseX - gridX) / SLOT_SIZE);
        int row = (int) Math.floor((mouseY - gridY) / SLOT_SIZE);
        if (col >= 0 && col < 9 && row >= 0 && row < 3) {
            clickSlot(storageItems, storageCounts, row * 9 + col);
            return;
        }
        int hotbarX = (windowWidth - HOTBAR_SLOTS * SLOT_SIZE) / 2;
        int hotbarY = windowHeight - SLOT_SIZE - 20;
        int hcol = (int) Math.floor((mouseX - hotbarX) / SLOT_SIZE);
        if (hcol >= 0 && hcol < HOTBAR_SLOTS
                && mouseY >= hotbarY && mouseY < hotbarY + SLOT_SIZE) {
            clickSlot(items, counts, hcol);
        }
    }

    /** Apply the pick-up / place / merge / swap rules to one slot. */
    private void clickSlot(int[] slotItems, int[] slotCounts, int i) {
        if (heldCount == 0) {
            if (slotCounts[i] > 0) {
                heldId = slotItems[i];
                heldCount = slotCounts[i];
                slotItems[i] = World.BLOCK_TYPE_AIR;
                slotCounts[i] = 0;
            }
        } else if (slotCounts[i] == 0) {
            slotItems[i] = heldId;
            slotCounts[i] = heldCount;
            heldId = World.BLOCK_TYPE_AIR;
            heldCount = 0;
        } else if (slotItems[i] == heldId) {
            int space = MAX_STACK - slotCounts[i];
            int moved = Math.min(space, heldCount);
            slotCounts[i] += moved;
            heldCount -= moved;
            if (heldCount == 0) {
                heldId = World.BLOCK_TYPE_AIR;
            }
        } else {
            int tmpId = slotItems[i];
            int tmpCount = slotCounts[i];
            slotItems[i] = heldId;
            slotCounts[i] = heldCount;
            heldId = tmpId;
            heldCount = tmpCount;
        }
    }

    /**
     * Return any stack still held by the mouse to the first free space
     * (called when the inventory screen closes so blocks are never lost).
     */
    public void returnHeldStack() {
        while (heldCount > 0 && addItem(BlockType.fromId(heldId))) {
            heldCount--;
        }
        heldId = World.BLOCK_TYPE_AIR;
        heldCount = 0;
    }

    private static int storageGridX(int windowWidth) {
        return (windowWidth - 9 * SLOT_SIZE) / 2;
    }

    private static int storageGridY(int windowHeight) {
        return (windowHeight - 3 * SLOT_SIZE) / 2 - 30;
    }

    /** Set up the ortho projection / GL state shared by all UI rendering. */
    private static void beginUi(int windowWidth, int windowHeight) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        TextureLoader.disableTextures();
    }

    /** Restore GL state after {@link #beginUi(int, int)}. */
    private static void endUi() {
        TextureLoader.enableTextures();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    /** Draw a single slot: background, border, item swatch and count. */
    private static void drawSlot(int slotX, int slotY, int id, int count, boolean highlighted) {
        if (highlighted) {
            GL11.glColor3f(0.8f, 0.8f, 0.8f);
        } else {
            GL11.glColor3f(0.5f, 0.5f, 0.5f);
        }

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(slotX, slotY);
        GL11.glVertex2f((float)slotX + SLOT_SIZE, slotY);
        GL11.glVertex2f((float)slotX + SLOT_SIZE, (float)slotY + SLOT_SIZE);
        GL11.glVertex2f(slotX, (float)slotY + SLOT_SIZE);
        GL11.glEnd();

        GL11.glColor3f(0.1f, 0.1f, 0.1f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(slotX, slotY);
        GL11.glVertex2f((float)slotX + SLOT_SIZE, slotY);
        GL11.glVertex2f((float)slotX + SLOT_SIZE, (float)slotY + SLOT_SIZE);
        GL11.glVertex2f(slotX, (float)slotY + SLOT_SIZE);
        GL11.glEnd();

        if (id != World.BLOCK_TYPE_AIR && count > 0) {
            int itemSize = 24;
            drawItemSwatch(slotX + (SLOT_SIZE - itemSize) / 2, slotY + (SLOT_SIZE - itemSize) / 2, id);
            if (count > 1) {
                GL11.glColor3f(1f, 1f, 1f);
                drawNumber(count, slotX + SLOT_SIZE - 4, slotY + SLOT_SIZE - 12);
            }
        }
    }

    /** Draw a 24×24 coloured square for the given block id. */
    private static void drawItemSwatch(int x, int y, int id) {
        int itemSize = 24;
        int rgb = BlockType.fromId(id).mapColor();
        GL11.glColor3f(((rgb >> 16) & 0xFF) / 255f,
                       ((rgb >> 8) & 0xFF) / 255f,
                       (rgb & 0xFF) / 255f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f((float)x + itemSize, y);
        GL11.glVertex2f((float)x + itemSize, (float)y + itemSize);
        GL11.glVertex2f(x, (float)y + itemSize);
        GL11.glEnd();
    }

    /** 3×5 pixel bitmaps for the digits 0-9, one bit per pixel, row-major. */
    private static final int[] DIGIT_ROWS = {
            0b111_101_101_101_111, // 0
            0b010_110_010_010_111, // 1
            0b111_001_111_100_111, // 2
            0b111_001_111_001_111, // 3
            0b101_101_111_001_001, // 4
            0b111_100_111_001_111, // 5
            0b111_100_111_101_111, // 6
            0b111_001_010_010_010, // 7
            0b111_101_111_101_111, // 8
            0b111_101_111_001_111, // 9
    };

    /**
     * Draw {@code value} as tiny 3×5 pixel digits, right-aligned so the last
     * digit ends at {@code rightX}. Assumes the caller already set colour and
     * the ortho projection used by {@link #render(int, int)}.
     */
    private static void drawNumber(int value, int rightX, int topY) {
        String s = Integer.toString(value);
        int pixel = 2;
        int digitW = 4 * pixel;
        int x = rightX - s.length() * digitW;
        GL11.glBegin(GL11.GL_QUADS);
        for (int c = 0; c < s.length(); c++) {
            int d = s.charAt(c) - '0';
            int bits = DIGIT_ROWS[d];
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    int bit = 14 - (row * 3 + col);
                    if (((bits >> bit) & 1) != 0) {
                        float px = x + (float) c * digitW + col * (float) pixel;
                        float py = topY + row * (float) pixel;
                        GL11.glVertex2f(px, py);
                        GL11.glVertex2f(px + pixel, py);
                        GL11.glVertex2f(px + pixel, py + pixel);
                        GL11.glVertex2f(px, py + pixel);
                    }
                }
            }
        }
        GL11.glEnd();
    }
}
