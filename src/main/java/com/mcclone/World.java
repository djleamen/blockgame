/**
 * The voxel world: block storage, generation, queries and rendering.
 *
 * <p>The world is a fixed-size dense {@code int[][][]} for now (Phase 1).
 * Each cell stores a {@link BlockType#id() block id}. Generation produces a
 * gentle heightmap with bedrock floor, stone fill, a dirt/grass surface,
 * sand at sea level and a sprinkling of small oak trees.</p>
 */

package com.mcclone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.lwjgl.opengl.GL11;

/**
 * The game world.
 *
 * <p>Coordinate conventions (unchanged from earlier phases so existing code
 * keeps working):</p>
 * <ul>
 *   <li>World-array X / Z are in {@code [0, SIZE)}.</li>
 *   <li>OpenGL X = {@code worldX - SIZE/2}, OpenGL Z = {@code -worldZ}.</li>
 *   <li>Y is up.</li>
 * </ul>
 *
 * <p>Rendering uses {@link #render()} which lazily builds and caches a list of
 * exposed faces grouped by texture, then submits each group as a single
 * {@code glBegin/glEnd} pair. This is still immediate-mode OpenGL but with
 * ~11 draw calls per frame instead of ~tens of thousands.</p>
 */
public class World {

    /** Horizontal world size in blocks. */
    public static final int SIZE = 64;

    /** Vertical world size in blocks. */
    public static final int HEIGHT = 64;

    /** Y of the water plane / typical low-ground level. */
    public static final int SEA_LEVEL = 12;

    // Legacy id constants kept so {@link Hotbar} and any existing code keep compiling.
    /** Air id. */ public static final int BLOCK_TYPE_AIR = BlockType.AIR.id();
    /** Grass id. */ public static final int BLOCK_TYPE_GRASS = BlockType.GRASS.id();
    /** Natural dirt id. */ public static final int BLOCK_TYPE_DIRT = BlockType.DIRT.id();
    /** Cobblestone id. */ public static final int BLOCK_TYPE_COBBLESTONE = BlockType.COBBLESTONE.id();
    /** Player-placed dirt id. */ public static final int BLOCK_TYPE_PLACED_DIRT = BlockType.PLACED_DIRT.id();
    /** Stone id. */ public static final int BLOCK_TYPE_STONE = BlockType.STONE.id();
    /** Sand id. */ public static final int BLOCK_TYPE_SAND = BlockType.SAND.id();
    /** Bedrock id. */ public static final int BLOCK_TYPE_BEDROCK = BlockType.BEDROCK.id();
    /** Oak log id. */ public static final int BLOCK_TYPE_OAK_LOG = BlockType.OAK_LOG.id();
    /** Oak planks id. */ public static final int BLOCK_TYPE_OAK_PLANKS = BlockType.OAK_PLANKS.id();
    /** Oak leaves id. */ public static final int BLOCK_TYPE_OAK_LEAVES = BlockType.OAK_LEAVES.id();
    /** Water id. */ public static final int BLOCK_TYPE_WATER = BlockType.WATER.id();
    /** Gravel id. */ public static final int BLOCK_TYPE_GRAVEL = BlockType.GRAVEL.id();
    /** Coal ore id. */ public static final int BLOCK_TYPE_COAL_ORE = BlockType.COAL_ORE.id();
    /** Iron ore id. */ public static final int BLOCK_TYPE_IRON_ORE = BlockType.IRON_ORE.id();

    /** Face direction constants used by {@link #addFace}. */
    private static final int FACE_TOP = 0;
    private static final int FACE_BOTTOM = 1;
    private static final int FACE_POS_Z = 2;
    private static final int FACE_NEG_Z = 3;
    private static final int FACE_NEG_X = 4;
    private static final int FACE_POS_X = 5;

    private static final long DEFAULT_SEED = 0x10C6A47BL;

    /** Texture key of the translucent water batch (drawn last, blended). */
    private static final String WATER_TEXTURE_KEY = "water";

    /**
     * Height of a still-water surface block, as a fraction of a full block.
     * Minecraft renders still liquids 2 pixels (2/16) below the block top so
     * the waterline reads clearly against neighbouring solid blocks; clones
     * universally copy the same 14/16 value.
     */
    private static final float WATER_SURFACE_HEIGHT = 14f / 16f;

    /** Horizontal size of a render/meshing chunk in blocks. */
    private static final int CHUNK_SIZE = 16;

    /** Number of chunks along the world X axis (ceil so any SIZE is covered). */
    private static final int CHUNKS_X = (SIZE + CHUNK_SIZE - 1) / CHUNK_SIZE;

    /** Number of chunks along the world Z axis (ceil so any SIZE is covered). */
    private static final int CHUNKS_Z = (SIZE + CHUNK_SIZE - 1) / CHUNK_SIZE;

    /** 3D array storing block ids [x][y][z]. */
    private final int[][][] blocks = new int[SIZE][HEIGHT][SIZE];

    /**
     * Per-chunk render batches. Each chunk owns its own {@code texture → packed
     * quad data} map and its own dirty flag, so a single block edit only forces
     * the containing chunk (and any bordering neighbour) to be re-meshed instead
     * of re-scanning the whole world.
     */
    private final Chunk[][] chunks = new Chunk[CHUNKS_X][CHUNKS_Z];

    /** Union of texture keys across all chunks, refreshed when chunks rebuild. */
    private final Set<String> textureKeys = new LinkedHashSet<>();

    {
        for (int cx = 0; cx < CHUNKS_X; cx++) {
            for (int cz = 0; cz < CHUNKS_Z; cz++) {
                chunks[cx][cz] = new Chunk(cx, cz);
            }
        }
    }

    /**
     * Construct and generate the default world.
     */
    public World() {
        this(DEFAULT_SEED);
    }

    /**
     * Construct and generate the world with the given seed.
     *
     * @param seed deterministic generation seed
     */
    public World(long seed) {
        generate(seed);
    }

    /**
     * Generate the world terrain and populate it with trees, ores, etc.
     * 
     * @param seed deterministic generation seed
     */
    private void generate(long seed) {
        Random rng = new Random(seed);
        int[][] heights = new int[SIZE][SIZE];
        float[][] coarse = noiseGrid(rng, 8);
        float[][] fine = noiseGrid(rng, 4);
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                float n = sampleBilinear(coarse, x, z, 8) * 0.7f
                        + sampleBilinear(fine, x, z, 4) * 0.3f;
                int h = SEA_LEVEL - 2 + Math.round(n * 12f);
                if (h < 1) h = 1;
                if (h > HEIGHT - 8) h = HEIGHT - 8;
                heights[x][z] = h;
            }
        }

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int h = heights[x][z];
                blocks[x][0][z] = BLOCK_TYPE_BEDROCK;
                for (int y = 1; y < h - 3; y++) {
                    blocks[x][y][z] = BLOCK_TYPE_STONE;
                }
                int dirtTop = Math.max(0, h - 1);
                for (int y = Math.max(1, h - 3); y <= dirtTop; y++) {
                    blocks[x][y][z] = BLOCK_TYPE_DIRT;
                }
                boolean beach = h <= SEA_LEVEL + 1;
                blocks[x][h][z] = beach ? BLOCK_TYPE_SAND : BLOCK_TYPE_GRASS;
                if (beach) {
                    for (int y = Math.max(1, h - 2); y < h; y++) {
                        blocks[x][y][z] = BLOCK_TYPE_SAND;
                    }
                }
                for (int y = h + 1; y <= SEA_LEVEL; y++) {
                    blocks[x][y][z] = BLOCK_TYPE_WATER;
                }
            }
        }

        placeGravelPatches(rng, heights);
        placeOreVeins(rng, heights);

        int treeCount = 16;
        int attempts = 0;
        while (treeCount > 0 && attempts < 200) {
            attempts++;
            int x = 3 + rng.nextInt(SIZE - 6);
            int z = 3 + rng.nextInt(SIZE - 6);
            int h = heights[x][z];
            if (blocks[x][h][z] == BLOCK_TYPE_GRASS && plantTree(x, h + 1, z)) {
                treeCount--;
            }
        }
        markAllDirty();
    }

    /**
     * Scatter a few disks of gravel over beach / sea-floor sand, the way
     * Minecraft beaches mix sand and gravel.
     * 
     * @param rng Random number generator
     * @param heights Height map of the terrain
     */
    private void placeGravelPatches(Random rng, int[][] heights) {
        int patches = 10;
        for (int i = 0; i < patches; i++) {
            int cx = rng.nextInt(SIZE);
            int cz = rng.nextInt(SIZE);
            int radius = 2 + rng.nextInt(2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    int x = cx + dx;
                    int z = cz + dz;
                    if (x < 0 || x >= SIZE || z < 0 || z >= SIZE) continue;
                    int h = heights[x][z];
                    if (blocks[x][h][z] == BLOCK_TYPE_SAND) {
                        blocks[x][h][z] = BLOCK_TYPE_GRAVEL;
                    }
                }
            }
        }
    }

    /**
     * Seed small ore veins in the stone layer: plenty of coal at any depth,
     * rarer iron biased toward the lower half of the stone.
     * 
     * @param rng Random number generator
     * @param heights Height map of the terrain
     */
    private void placeOreVeins(Random rng, int[][] heights) {
        placeVeinType(rng, heights, BLOCK_TYPE_COAL_ORE, 28, 4, 5, 1.0f);
        placeVeinType(rng, heights, BLOCK_TYPE_IRON_ORE, 16, 2, 4, 0.55f);
    }

    /**
     * Place a number of veins of a single ore type in the stone layer.
     * 
     * @param rng Random number generator
     * @param heights Height map of the terrain
     * @param oreId ID of the ore block
     * @param veins Number of veins to generate
     * @param minSize Minimum size of each vein
     * @param sizeRange Range of additional size for each vein
     * @param maxDepthFraction Maximum depth fraction of the stone layer for vein placement
     */
    private void placeVeinType(Random rng, int[][] heights, int oreId, int veins,
                               int minSize, int sizeRange, float maxDepthFraction) {
        for (int i = 0; i < veins; i++) {
            int x = rng.nextInt(SIZE);
            int z = rng.nextInt(SIZE);
            int stoneTop = heights[x][z] - 4;
            if (stoneTop < 2) continue;
            int maxY = Math.max(2, Math.round(stoneTop * maxDepthFraction));
            int y = 1 + rng.nextInt(maxY);
            int size = minSize + rng.nextInt(sizeRange);
            int bx = x;
            int by = y;
            int bz = z;
            for (int n = 0; n < size; n++) {
                if (inBounds(bx, by, bz) && blocks[bx][by][bz] == BLOCK_TYPE_STONE) {
                    blocks[bx][by][bz] = oreId;
                }
                // Random walk to the next cell of the vein.
                switch (rng.nextInt(6)) {
                    case 0: bx++; break;
                    case 1: bx--; break;
                    case 2: by++; break;
                    case 3: by--; break;
                    case 4: bz++; break;
                    default: bz--; break;
                }
            }
        }
    }

    /**
     * Plant a small oak tree at the given location if there's enough space.
     * 
     * @param x X-coordinate of the tree's base
     * @param y Y-coordinate of the tree's base
     * @param z Z-coordinate of the tree's base
     * @return true if the tree was successfully planted
     */
    private boolean plantTree(int x, int y, int z) {
        int trunkH = 4 + (Math.abs(x * 31 + z) % 2);
        if (y + trunkH + 1 >= HEIGHT) return false;
        for (int i = 0; i < trunkH; i++) {
            blocks[x][y + i][z] = BLOCK_TYPE_OAK_LOG;
        }
        int topY = y + trunkH;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    int lx = x + dx;
                    int lz = z + dz;
                    int ly = topY + dy;
                    if (inBounds(lx, ly, lz) && blocks[lx][ly][lz] == BLOCK_TYPE_AIR) {
                        blocks[lx][ly][lz] = BLOCK_TYPE_OAK_LEAVES;
                    }
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 1) continue;
                int lx = x + dx;
                int lz = z + dz;
                int ly = topY + 1;
                if (inBounds(lx, ly, lz) && blocks[lx][ly][lz] == BLOCK_TYPE_AIR) {
                    blocks[lx][ly][lz] = BLOCK_TYPE_OAK_LEAVES;
                }
            }
        }
        return true;
    }

    /**
     * Generate a grid of random float values for use in terrain noise generation.
     * 
     * @param rng Random number generator
     * @param step Step size for the grid
     * @return A 2D array of random float values
     */
    private static float[][] noiseGrid(Random rng, int step) {
        int n = SIZE / step + 2;
        float[][] g = new float[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                g[i][j] = rng.nextFloat();
            }
        }
        return g;
    }

    /**
     * Sample a 2D grid of float values using bilinear interpolation.
     * 
     * @param grid The 2D array of float values to sample from
     * @param x X-coordinate in the grid
     * @param z Z-coordinate in the grid
     * @param step Step size used to generate the grid
     * @return The interpolated float value at the given coordinates
     */
    private static float sampleBilinear(float[][] grid, int x, int z, int step) {
        float fx = x / (float) step;
        float fz = z / (float) step;
        int x0 = (int) Math.floor(fx);
        int z0 = (int) Math.floor(fz);
        int x1 = Math.min(grid.length - 1, x0 + 1);
        int z1 = Math.min(grid[0].length - 1, z0 + 1);
        float tx = fx - x0;
        float tz = fz - z0;
        float sx = tx * tx * (3 - 2 * tx);
        float sz = tz * tz * (3 - 2 * tz);
        float a = lerp(grid[x0][z0], grid[x1][z0], sx);
        float b = lerp(grid[x0][z1], grid[x1][z1], sx);
        return lerp(a, b, sz);
    }

    /**
     * Linearly interpolate between two float values.
     *
     * @param a The start value
     * @param b The end value
     * @param t The interpolation factor in [0, 1]
     * @return The interpolated value
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Render the world.
     *
     * <p>Faces are grouped by texture into batches. Each batch is a flat
     * {@code float[]} with 5 floats per vertex (u, v, x, y, z) and 4 vertices
     * per quad. Batches are partitioned per chunk; only dirty chunks are
     * re-meshed before drawing. Each texture is bound once and every chunk's
     * batch for that texture is emitted inside a single {@code glBegin/glEnd}
     * pair, keeping the per-frame bind count at ~one per texture.</p>
     */
    public void render() {
        render(1f);
    }

    /**
     * Render the world with a global brightness multiplier (1 = full daylight,
     * lower values darken every face uniformly — driven by
     * {@link DayNightCycle}).
     *
     * @param brightness light multiplier in {@code (0, 1]}
     */
    public void render(float brightness) {
        rebuildDirtyChunks();
        TextureLoader.enableTextures();
        GL11.glColor3f(brightness, brightness, brightness);
        for (String key : textureKeys) {
            if (WATER_TEXTURE_KEY.equals(key)) continue;
            drawBatches(key);
        }
        if (textureKeys.contains(WATER_TEXTURE_KEY)) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
            drawBatches(WATER_TEXTURE_KEY);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
        }
        GL11.glColor3f(1f, 1f, 1f);
    }

    /** Bind {@code key}'s texture and emit every chunk's batch for it. */
    private void drawBatches(String key) {
        int tex = TextureLoader.getTexture(key);
        TextureLoader.bindTexture(tex);
        GL11.glBegin(GL11.GL_QUADS);
        for (int cx = 0; cx < CHUNKS_X; cx++) {
            for (int cz = 0; cz < CHUNKS_Z; cz++) {
                float[] verts = chunks[cx][cz].batches.get(key);
                if (verts == null) continue;
                for (int i = 0; i < verts.length; i += 5) {
                    GL11.glTexCoord2f(verts[i], verts[i + 1]);
                    GL11.glVertex3f(verts[i + 2], verts[i + 3], verts[i + 4]);
                }
            }
        }
        GL11.glEnd();
    }

    /**
     * Re-mesh every chunk whose dirty flag is set, then refresh the union of
     * texture keys if anything changed.
     */
    private void rebuildDirtyChunks() {
        boolean any = false;
        for (int cx = 0; cx < CHUNKS_X; cx++) {
            for (int cz = 0; cz < CHUNKS_Z; cz++) {
                Chunk c = chunks[cx][cz];
                if (c.dirty) {
                    rebuildChunk(c);
                    c.dirty = false;
                    any = true;
                }
            }
        }
        if (any) {
            textureKeys.clear();
            for (int cx = 0; cx < CHUNKS_X; cx++) {
                for (int cz = 0; cz < CHUNKS_Z; cz++) {
                    textureKeys.addAll(chunks[cx][cz].batches.keySet());
                }
            }
        }
    }

    /**
     * Build the texture-grouped face batches for a single chunk. Face culling
     * still reads neighbouring cells directly from {@link #blocks}, so faces on
     * a chunk border are correctly hidden/exposed against the adjacent chunk.
     *
     * @param c the chunk to re-mesh
     */
    private void rebuildChunk(Chunk c) {
        Map<String, FaceList> builders = new HashMap<>();
        float ox = -SIZE / 2f;
        int x0 = c.cx * CHUNK_SIZE;
        int x1 = Math.min(SIZE, x0 + CHUNK_SIZE);
        int z0 = c.cz * CHUNK_SIZE;
        int z1 = Math.min(SIZE, z0 + CHUNK_SIZE);
        for (int x = x0; x < x1; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = z0; z < z1; z++) {
                    int id = blocks[x][y][z];
                    if (id == BLOCK_TYPE_AIR) continue;
                    BlockType type = BlockType.fromId(id);
                    float wx = x + ox;
                    float wy = y;
                    float wz = -z;

                    // Surface water (no water directly above) is rendered
                    // slightly shorter than a full block, like Minecraft, so
                    // the waterline is visible against the surrounding land.
                    boolean waterAbove = inBounds(x, y + 1, z)
                            && blocks[x][y + 1][z] == BLOCK_TYPE_WATER;
                    float top = (id == BLOCK_TYPE_WATER && !waterAbove)
                            ? WATER_SURFACE_HEIGHT : 1f;

                    if (showFace(id, x, y + 1, z)) addFace(builders, type, FACE_TOP, wx, wy, wz, top);
                    if (showFace(id, x, y - 1, z)) addFace(builders, type, FACE_BOTTOM, wx, wy, wz, top);
                    if (showFace(id, x, y, z - 1)) addFace(builders, type, FACE_POS_Z, wx, wy, wz, top);
                    if (showFace(id, x, y, z + 1)) addFace(builders, type, FACE_NEG_Z, wx, wy, wz, top);
                    if (showFace(id, x - 1, y, z)) addFace(builders, type, FACE_NEG_X, wx, wy, wz, top);
                    if (showFace(id, x + 1, y, z)) addFace(builders, type, FACE_POS_X, wx, wy, wz, top);
                }
            }
        }
        Map<String, float[]> result = new HashMap<>();
        for (Map.Entry<String, FaceList> e : builders.entrySet()) {
            result.put(e.getKey(), e.getValue().toArray());
        }
        c.batches = result;
    }

    /**
     * Face-culling test: should block {@code id}'s face toward the neighbour
     * cell be emitted? Opaque neighbours hide the face; two adjacent water
     * blocks also hide their shared faces so the sea isn't full of internal
     * quads.
     * 
     * @param id the block id of the face's owner
     * @param nx neighbour X
     * @param ny neighbour Y
     * @param nz neighbour Z
     * @return true if the face should be emitted, false if it is hidden
     */
    private boolean showFace(int id, int nx, int ny, int nz) {
        if (isOpaque(nx, ny, nz)) return false;
        if (id == BLOCK_TYPE_WATER && inBounds(nx, ny, nz)
                && blocks[nx][ny][nz] == BLOCK_TYPE_WATER) {
            return false;
        }
        return true;
    }

    /** 
     * Mark every chunk dirty (used after full-world generation). 
     */
    private void markAllDirty() {
        for (int cx = 0; cx < CHUNKS_X; cx++) {
            for (int cz = 0; cz < CHUNKS_Z; cz++) {
                chunks[cx][cz].dirty = true;
            }
        }
    }

    /**
     * Mark the chunk containing world column ({@code x}, {@code z}) dirty, plus
     * any neighbouring chunk when the column lies on a chunk border (face
     * culling for the neighbour reads across the boundary).
     *
     * @param x world-array X of the edited cell
     * @param z world-array Z of the edited cell
     */
    private void markDirty(int x, int z) {
        int cx = x / CHUNK_SIZE;
        int cz = z / CHUNK_SIZE;
        chunks[cx][cz].dirty = true;
        int lx = x % CHUNK_SIZE;
        int lz = z % CHUNK_SIZE;
        if (lx == 0 && cx > 0) chunks[cx - 1][cz].dirty = true;
        if (lx == CHUNK_SIZE - 1 && cx < CHUNKS_X - 1) chunks[cx + 1][cz].dirty = true;
        if (lz == 0 && cz > 0) chunks[cx][cz - 1].dirty = true;
        if (lz == CHUNK_SIZE - 1 && cz < CHUNKS_Z - 1) chunks[cx][cz + 1].dirty = true;
    }

    /**
     * Emit one face of a block into the texture-grouped builders.
     *
     * @param top height of the block's top edge as a fraction of a full block
     *            (1 for normal blocks, {@link #WATER_SURFACE_HEIGHT} for a
     *            water surface). Side faces have their texture V range scaled
     *            to match so the texels aren't stretched.
     */
    private static void addFace(Map<String, FaceList> builders, BlockType type, int face,
                                float wx, float wy, float wz, float top) {
        String key;
        switch (face) {
            case FACE_TOP: key = type.topTexture(); break;
            case FACE_BOTTOM: key = type.bottomTexture(); break;
            default: key = type.sideTexture(); break;
        }
        if (key == null) return;
        FaceList list = builders.computeIfAbsent(key, k -> new FaceList());
        float topY = wy + top;
        float vTop = 1f - top; // texture V at the (possibly lowered) top edge
        // Each face is 4 verts × 5 floats. Block occupies wx..wx+1, wy..topY, wz..wz-1.
        switch (face) {
            case FACE_TOP:
                list.quad(
                        0, 0, wx,     topY, wz,
                        1, 0, wx + 1, topY, wz,
                        1, 1, wx + 1, topY, wz - 1,
                        0, 1, wx,     topY, wz - 1);
                break;
            case FACE_BOTTOM:
                list.quad(
                        0, 1, wx,     wy, wz,
                        1, 1, wx + 1, wy, wz,
                        1, 0, wx + 1, wy, wz - 1,
                        0, 0, wx,     wy, wz - 1);
                break;
            case FACE_POS_Z:
                list.quad(
                        0, 1,    wx,     wy,   wz,
                        1, 1,    wx + 1, wy,   wz,
                        1, vTop, wx + 1, topY, wz,
                        0, vTop, wx,     topY, wz);
                break;
            case FACE_NEG_Z:
                list.quad(
                        1, 1,    wx + 1, wy,   wz - 1,
                        0, 1,    wx,     wy,   wz - 1,
                        0, vTop, wx,     topY, wz - 1,
                        1, vTop, wx + 1, topY, wz - 1);
                break;
            case FACE_NEG_X:
                list.quad(
                        1, 1,    wx, wy,   wz - 1,
                        0, 1,    wx, wy,   wz,
                        0, vTop, wx, topY, wz,
                        1, vTop, wx, topY, wz - 1);
                break;
            case FACE_POS_X:
                list.quad(
                        0, 1,    wx + 1, wy,   wz,
                        1, 1,    wx + 1, wy,   wz - 1,
                        1, vTop, wx + 1, topY, wz - 1,
                        0, vTop, wx + 1, topY, wz);
                break;
            default:
                break;
        }
    }

    /**
     * Find the Y of the topmost solid block above ({@code x}, {@code z}) in
     * world-space coordinates and return the floor height above it.
     *
     * @param x world-space X
     * @param z world-space Z
     * @return the Y at which an entity would stand on the ground
     */
    public float groundHeight(float x, float z) {
        int bx = (int) Math.floor(x + SIZE / 2f);
        int bz = (int) Math.floor(-z);
        for (int y = HEIGHT - 1; y >= 0; y--) {
            if (isSolid(bx, y, bz)) {
                return y + 1f;
            }
        }
        return 0f;
    }

    /**
     * Find the top of the highest block in column ({@code x}, {@code z}) whose
     * top is at or below {@code maxFeetY}. This is the right query for gravity:
     * we want the ground the player would land on from their current position,
     * not the canopy hanging overhead.
     *
     * @param x         world-space X
     * @param maxFeetY  player's current feet Y; only blocks with {@code top <= maxFeetY} count
     * @param z         world-space Z
     * @return the standing Y of the next block down, or
     *         {@link Float#NEGATIVE_INFINITY} if the column is empty / out of
     *         bounds (used by the caller to detect "falling into the void")
     */
    public float groundHeightBelow(float x, float maxFeetY, float z) {
        int bx = (int) Math.floor(x + SIZE / 2f);
        int bz = (int) Math.floor(-z);
        if (bx < 0 || bx >= SIZE || bz < 0 || bz >= SIZE) {
            return Float.NEGATIVE_INFINITY;
        }
        // The highest by such that (by + 1) <= maxFeetY is floor(maxFeetY - 1).
        int startY = Math.min(HEIGHT - 1, (int) Math.floor(maxFeetY - 1));
        for (int by = startY; by >= 0; by--) {
            if (isSolid(bx, by, bz)) {
                return by + 1f;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Place a block of the given type at the given coordinates if the space is air.
     *
     * @param x         block x
     * @param y         block y
     * @param z         block z
     * @param blockType block type
     */
    public void setBlock(int x, int y, int z, BlockType blockType) {
        if (inBounds(x, y, z)) {
            int oldId = blocks[x][y][z];
            int newId = (blockType != null) ? blockType.id() : BLOCK_TYPE_AIR;

            if (oldId == newId) return;

            if (oldId == BLOCK_TYPE_BEDROCK) return;

            blocks[x][y][z] = newId;

            // If we just placed a block, check if a grass block below it should become dirt.
            if (newId != BLOCK_TYPE_AIR) {
                if (inBounds(x, y - 1, z) && blocks[x][y - 1][z] == BLOCK_TYPE_GRASS) {
                    blocks[x][y - 1][z] = BLOCK_TYPE_DIRT;
                }
            }

            // If we just broke a block, check if dirt below it should become grass.
            if (newId == BLOCK_TYPE_AIR) {
                if (inBounds(x, y - 1, z) && blocks[x][y - 1][z] == BLOCK_TYPE_DIRT) {
                    if (!isOpaque(x, y, z)) { // check if there is now air above
                        blocks[x][y - 1][z] = BLOCK_TYPE_GRASS;
                    }
                }
            }

            markDirty(x, z);
        }
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < HEIGHT && z >= 0 && z < SIZE;
    }

    /**
     * @return true if the cell holds a non-air block.
     */
    public boolean hasBlock(int x, int y, int z) {
        return inBounds(x, y, z) && blocks[x][y][z] != BLOCK_TYPE_AIR;
    }

    /**
     * @return true if the cell holds a solid (collidable / targetable) block.
     *         Water is present but not solid, so entities pass through it and
     *         the crosshair raycast ignores it.
     */
    public boolean isSolid(int x, int y, int z) {
        return inBounds(x, y, z) && BlockType.fromId(blocks[x][y][z]).isSolid();
    }

    /**
     * @return the {@link BlockType} stored at the given cell, or
     *         {@link BlockType#AIR} when out of bounds.
     */
    public BlockType blockAt(int x, int y, int z) {
        if (!inBounds(x, y, z)) return BlockType.AIR;
        return BlockType.fromId(blocks[x][y][z]);
    }

    /**
     * Write the block array as run-length encoded ({@code id}, {@code count})
     * int pairs in x→y→z scan order. Consumed by {@link WorldSave}.
     *
     * @param out destination stream
     * @throws IOException on write failure
     */
    public void writeBlocks(DataOutputStream out) throws IOException {
        int runId = blocks[0][0][0];
        int runLen = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    int id = blocks[x][y][z];
                    if (id == runId) {
                        runLen++;
                    } else {
                        out.writeInt(runId);
                        out.writeInt(runLen);
                        runId = id;
                        runLen = 1;
                    }
                }
            }
        }
        out.writeInt(runId);
        out.writeInt(runLen);
    }

    /**
     * Replace the world's blocks with run-length encoded data produced by
     * {@link #writeBlocks(DataOutputStream)} and re-mesh everything.
     *
     * @param in source stream
     * @throws IOException on read failure or malformed payload
     */
    public void readBlocks(DataInputStream in) throws IOException {
        int total = SIZE * HEIGHT * SIZE;
        int filled = 0;
        int x = 0;
        int y = 0;
        int z = 0;
        while (filled < total) {
            int id = in.readInt();
            int len = in.readInt();
            if (len <= 0 || filled + len > total || BlockType.fromId(id).id() != id) {
                throw new IOException("Corrupt RLE block payload");
            }
            for (int n = 0; n < len; n++) {
                blocks[x][y][z] = id;
                z++;
                if (z == SIZE) { z = 0; y++; }
                if (y == HEIGHT) { y = 0; x++; }
            }
            filled += len;
        }
        markAllDirty();
    }

    /**
     * @return true if the block at the given position fully hides neighbouring faces.
     *         Out-of-bounds <em>below</em> y=0 is treated as opaque so the bedrock
     *         floor doesn't render its bottom; everything else out of bounds is
     *         transparent.
     */
    public boolean isOpaque(int x, int y, int z) {
        if (y < 0) return true;
        if (!inBounds(x, y, z)) return false;
        return BlockType.fromId(blocks[x][y][z]).isOpaque();
    }

    /**
     * A 16×16 column of the world (full height) holding its own render batches
     * and dirty flag. Re-meshing is scoped to one chunk at a time.
     */
    private static final class Chunk {
        final int cx;
        final int cz;
        Map<String, float[]> batches = new HashMap<>();
        boolean dirty = true;

        Chunk(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }
    }

    /**
     * A simple float[] wrapper that grows as needed.
     */
    private static class FaceList {
        float[] data = new float[128];
        int size = 0;

        void quad(float u1, float v1, float x1, float y1, float z1,
                  float u2, float v2, float x2, float y2, float z2,
                  float u3, float v3, float x3, float y3, float z3,
                  float u4, float v4, float x4, float y4, float z4) {
            if (size + 20 > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            int i = size;
            data[i++] = u1; data[i++] = v1; data[i++] = x1; data[i++] = y1; data[i++] = z1;
            data[i++] = u2; data[i++] = v2; data[i++] = x2; data[i++] = y2; data[i++] = z2;
            data[i++] = u3; data[i++] = v3; data[i++] = x3; data[i++] = y3; data[i++] = z3;
            data[i++] = u4; data[i++] = v4; data[i++] = x4; data[i++] = y4; data[i++] = z4;
            size = i;
        }

        float[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
