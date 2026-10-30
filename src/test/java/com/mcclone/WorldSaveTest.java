package com.mcclone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSaveTest {

    @TempDir
    Path dir;

    private static boolean save(File file, World world) {
        return WorldSave.save(file, world, new Player(1f, 40f, 2f), new Hotbar(), new DayNightCycle());
    }

    private static boolean load(File file, World world) {
        return WorldSave.load(file, world, new Player(0f, 40f, 0f), new Hotbar(), new DayNightCycle());
    }

    private static boolean sameBlocks(World a, World b) {
        for (int x = 0; x < World.SIZE; x++) {
            for (int y = 0; y < World.HEIGHT; y++) {
                for (int z = 0; z < World.SIZE; z++) {
                    if (a.blockAt(x, y, z) != b.blockAt(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Test
    void roundTripRestoresEditedBlocks() {
        File file = dir.resolve("world.bgsave").toFile();
        World edited = new World();
        edited.setBlock(10, World.HEIGHT - 2, 10, BlockType.fromId(World.BLOCK_TYPE_STONE));
        assertTrue(save(file, edited));

        World loaded = new World();
        assertTrue(load(file, loaded));
        assertTrue(sameBlocks(edited, loaded));
    }

    @Test
    void truncatedSaveLeavesWorldUntouchedAndIsKept() throws IOException {
        File file = dir.resolve("world.bgsave").toFile();
        World other = new World(42L);
        assertTrue(save(file, other));
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(raf.length() / 2);
        }

        World current = new World();
        assertFalse(load(file, current));
        assertTrue(sameBlocks(new World(), current), "a failed load must not half-overwrite the world");

        assertFalse(file.exists(), "unreadable save is moved aside so autosave can't clobber it");
        File[] backups = dir.toFile().listFiles((d, name) -> name.startsWith("world.bgsave.unreadable-"));
        assertEquals(1, backups.length);
    }
}
