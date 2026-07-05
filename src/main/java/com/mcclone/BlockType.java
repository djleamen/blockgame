/**
 * Catalogue of every block type in the world.
 *
 * <p>Block types are stored in the world as plain {@code int} ids (to keep the
 * dense {@code int[][][]} representation in {@link World}), but this enum is
 * the source of truth for the id values, display names and texture keys used
 * when rendering each face.</p>
 */

package com.mcclone;

/**
 * Enumeration of all block kinds known to the game.
 *
 * <p>Each value carries:</p>
 * <ul>
 *   <li>a stable numeric {@link #id()} used everywhere the world stores blocks,</li>
 *   <li>texture keys for the top, side and bottom face (the keys map onto
 *       textures registered with {@link TextureLoader}),</li>
 *   <li>a small set of behaviour flags (solid, opaque) that drive lighting and
 *       collision in later phases.</li>
 * </ul>
 *
 * <p>The numeric ids are deliberately kept compatible with the legacy
 * {@code World.BLOCK_TYPE_*} constants so older serialised data and the
 * existing {@link Hotbar} continue to work.</p>
 */
public enum BlockType {

    /** Empty space. Never rendered. */
    AIR(0, null, null, null, false, false, 0x000000),

    /** Standard grass block — green top, dirt bottom, grassy sides. */
    GRASS(1, "grass_top", "grass_side", "dirt", true, true, 0x66CC33),

    /** Naturally generated dirt (auto-converts to grass when uncovered). */
    DIRT(2, "dirt", "dirt", "dirt", true, true, 0x996633),

    /** Cobblestone. */
    COBBLESTONE(3, "cobblestone", "cobblestone", "cobblestone", true, true, 0x666666),

    /** Player-placed dirt — never auto-converts to grass. */
    PLACED_DIRT(4, "dirt", "dirt", "dirt", true, true, 0x996633),

    /** Plain stone, the bulk of the underground. */
    STONE(5, "stone", "stone", "stone", true, true, 0x7F7F7F),

    /** Sand. */
    SAND(6, "sand", "sand", "sand", true, true, 0xDBCFA3),

    /** Bedrock — unbreakable floor block. */
    BEDROCK(7, "bedrock", "bedrock", "bedrock", true, true, 0x454545),

    /** Oak log — bark on the sides, growth rings top and bottom. */
    OAK_LOG(8, "log_oak_top", "log_oak_side", "log_oak_top", true, true, 0x5E442A),

    /** Oak planks. */
    OAK_PLANKS(9, "planks_oak", "planks_oak", "planks_oak", true, true, 0xB28E58),

    /** Oak leaves — opaque for now (no transparency sorting yet). */
    OAK_LEAVES(10, "leaves_oak", "leaves_oak", "leaves_oak", true, true, 0x386E26),

    /** Still water — translucent, non-solid, never collected or placed by hand. */
    WATER(11, "water", "water", "water", false, false, 0x3F76E4),

    /** Gravel. */
    GRAVEL(12, "gravel", "gravel", "gravel", true, true, 0x88807E),

    /** Coal ore — stone with black flecks. */
    COAL_ORE(13, "coal_ore", "coal_ore", "coal_ore", true, true, 0x4A4A4A),

    /** Iron ore — stone with tan flecks. */
    IRON_ORE(14, "iron_ore", "iron_ore", "iron_ore", true, true, 0xD8AF93);

    private static final BlockType[] BY_ID;
    static {
        int max = 0;
        for (BlockType t : values()) {
            max = Math.max(max, t.id);
        }
        BY_ID = new BlockType[max + 1];
        for (BlockType t : values()) {
            BY_ID[t.id] = t;
        }
    }

    private final int id;
    private final String topTexture;
    private final String sideTexture;
    private final String bottomTexture;
    private final boolean solid;
    private final boolean opaque;
    private final int mapColor;

    BlockType(int id, String topTexture, String sideTexture, String bottomTexture,
              boolean solid, boolean opaque, int mapColor) {
        this.id = id;
        this.topTexture = topTexture;
        this.sideTexture = sideTexture;
        this.bottomTexture = bottomTexture;
        this.solid = solid;
        this.opaque = opaque;
        this.mapColor = mapColor;
    }

    /** @return the stable numeric id stored in the world array. */
    public int id() { return id; }

    /** @return texture key for the +Y face. */
    public String topTexture() { return topTexture; }

    /** @return texture key for the four side faces. */
    public String sideTexture() { return sideTexture; }

    /** @return texture key for the -Y face. */
    public String bottomTexture() { return bottomTexture; }

    /** @return true if entities should collide with this block. */
    public boolean isSolid() { return solid; }

    /** @return true if this block fully hides faces of neighbours behind it. */
    public boolean isOpaque() { return opaque; }

    /** @return a representative 0xRRGGBB colour used for UI swatches. */
    public int mapColor() { return mapColor; }

    /**
     * What a player receives when they break this block, mirroring classic
     * Minecraft drops: stone drops cobblestone, grass drops dirt, leaves drop
     * nothing, everything else drops itself.
     *
     * @return the block type added to the inventory, or {@link #AIR} for no drop
     */
    public BlockType dropType() {
        switch (this) {
            case STONE:       return COBBLESTONE;
            case GRASS:       return PLACED_DIRT;
            case DIRT:        return PLACED_DIRT;
            case OAK_LEAVES:  return AIR;
            case WATER:       return AIR;
            case BEDROCK:     return AIR;
            default:          return this;
        }
    }

    /**
     * Look up a {@link BlockType} by its numeric id.
     *
     * @param id the id stored in the world array
     * @return the matching type, or {@link #AIR} if the id is unknown
     */
    public static BlockType fromId(int id) {
        if (id < 0 || id >= BY_ID.length || BY_ID[id] == null) {
            return AIR;
        }
        return BY_ID[id];
    }
}
