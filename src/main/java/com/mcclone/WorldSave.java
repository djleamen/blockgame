/**
 * World persistence: saving and loading the game to a single `.bgsave` file.
 */

package com.mcclone;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes the complete game state (blocks, player pose, hotbar) to a
 * compact binary file.
 *
 * <p>Format (all big-endian, via {@link DataOutputStream}):</p>
 * <pre>
 *   int    magic   "BGSV" (0x42475356)
 *   int    version (2)
 *   int    world SIZE, int world HEIGHT   (must match the running build)
 *   float  player x, y, z, pitch, yaw
 *   float  time of day in cycle seconds
 *   int    selected hotbar slot
 *   9 ×  ( int blockId, int count )       hotbar slots
 *   27 × ( int blockId, int count )       inventory storage slots (v2+)
 *   RLE block payload: pairs of ( int blockId, int runLength ) in x→y→z
 *   scan order until SIZE*HEIGHT*SIZE cells have been emitted.
 * </pre>
 */
public final class WorldSave {

    private static final Logger logger = LoggerFactory.getLogger(WorldSave.class);

    private static final int MAGIC = 0x42475356; // "BGSV"
    private static final int VERSION = 2;

    /** Default save location, in the game's working directory. */
    public static final File DEFAULT_FILE = new File("world.bgsave");

    private WorldSave() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Save the full game state. Writes to a temp file first and renames so a
     * crash mid-write can't corrupt an existing save.
     *
     * @return true if the save succeeded
     */
    public static boolean save(File file, World world, Player player, Hotbar hotbar,
                               DayNightCycle dayNight) {
        File tmp = new File(file.getPath() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(tmp)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(World.SIZE);
            out.writeInt(World.HEIGHT);

            out.writeFloat(player.getX());
            out.writeFloat(player.getY());
            out.writeFloat(player.getZ());
            out.writeFloat(player.getPitch());
            out.writeFloat(player.getYaw());

            out.writeFloat(dayNight.timeSeconds());

            out.writeInt(hotbar.getSelectedSlot());
            for (int i = 0; i < Hotbar.slotCount(); i++) {
                out.writeInt(hotbar.getItemId(i));
                out.writeInt(hotbar.getCount(i));
            }
            for (int i = 0; i < Hotbar.storageSlotCount(); i++) {
                out.writeInt(hotbar.getStorageId(i));
                out.writeInt(hotbar.getStorageCount(i));
            }

            world.writeBlocks(out);
        } catch (IOException e) {
            logger.warn("Failed to write save file {}: {}", tmp, e.getMessage());
            return false;
        }
        try {
            java.nio.file.Files.move(tmp.toPath(), file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.warn("Failed to move save into place: {}", e.getMessage());
            return false;
        }
        logger.info("World saved to {}", file);
        return true;
    }

    /**
     * Load the full game state from {@code file}, replacing the world's blocks
     * and the player/hotbar state.
     *
     * @return true if the load succeeded; false if the file is missing,
     *         corrupt, or from an incompatible world size
     */
    public static boolean load(File file, World world, Player player, Hotbar hotbar,
                               DayNightCycle dayNight) {
        if (!file.isFile()) {
            return false;
        }
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            if (in.readInt() != MAGIC) {
                logger.warn("Save {} has bad magic — ignoring", file);
                return false;
            }
            int version = in.readInt();
            if (version < 1 || version > VERSION) {
                logger.warn("Save {} has unsupported version {} — ignoring", file, version);
                return false;
            }
            int size = in.readInt();
            int height = in.readInt();
            if (size != World.SIZE || height != World.HEIGHT) {
                logger.warn("Save {} is for a {}×{} world (need {}×{}) — ignoring",
                        file, size, height, World.SIZE, World.HEIGHT);
                return false;
            }

            float x = in.readFloat();
            float y = in.readFloat();
            float z = in.readFloat();
            float pitch = in.readFloat();
            float yaw = in.readFloat();

            float timeOfDay = in.readFloat();

            int selected = in.readInt();
            int[] ids = new int[Hotbar.slotCount()];
            int[] counts = new int[Hotbar.slotCount()];
            for (int i = 0; i < Hotbar.slotCount(); i++) {
                ids[i] = in.readInt();
                counts[i] = in.readInt();
            }

            int[] storageIds = new int[Hotbar.storageSlotCount()];
            int[] storageCounts = new int[Hotbar.storageSlotCount()];
            if (version >= 2) {
                for (int i = 0; i < Hotbar.storageSlotCount(); i++) {
                    storageIds[i] = in.readInt();
                    storageCounts[i] = in.readInt();
                }
            }

            world.readBlocks(in);

            player.setPose(x, y, z, pitch, yaw);
            dayNight.setTimeSeconds(timeOfDay);
            hotbar.selectSlot(selected);
            for (int i = 0; i < Hotbar.slotCount(); i++) {
                hotbar.setSlot(i, ids[i], counts[i]);
            }
            for (int i = 0; i < Hotbar.storageSlotCount(); i++) {
                hotbar.setStorageSlot(i, storageIds[i], storageCounts[i]);
            }
            logger.info("World loaded from {}", file);
            return true;
        } catch (IOException e) {
            logger.warn("Failed to read save file {}: {}", file, e.getMessage());
            return false;
        }
    }
}
