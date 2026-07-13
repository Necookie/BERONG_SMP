package net.necookie.disastersim.common.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.common.structure.SimulationStructureLoader;
import net.necookie.disastersim.tutorial.NpcRole;
import net.necookie.disastersim.tutorial.TutorialManager;

import java.util.UUID;

/**
 * Manages the BFP tutorial lobby: programmatic structure building and NPC spawning.
 *
 * <p>On server start, attempts to load {@code bfp_tutorial_lobby.nbt} first; if the
 * file is absent the lobby is built from scratch with {@link #buildStructure}. Three
 * named Villager NPCs are tagged via persistent data so that the entity-interact handler
 * in {@link LobbyManager} can dispatch clicks to {@link TutorialManager#onNpcInteract}.
 *
 * <p>Room layout — X 0-19 (20 wide), Z 0-39 (40 long), Y 0-6 (7 tall):
 * <pre>
 *   Z  0     : Front entrance wall (4-wide door at X=8-11)
 *   Z  1- 9  : Entry hall — Sgt. Reyes (TRAINER) at X=4, Z=5
 *   Z 10     : Section divider (passage X=9-11)
 *   Z 11-20  : Fire practice area (campfires at offset X=12, Z=15)
 *   Z 21     : Section divider
 *   Z 22-30  : Extinguisher types — Officer Cruz (EXT_EXPERT) at X=4, Z=25
 *   Z 31     : Section divider
 *   Z 32-39  : Earthquake drill zone — Capt. Santos (SAFETY_OFFICER) at X=4, Z=35
 * </pre>
 */
public class TutorialLobbyManager {

    /** World origin of the tutorial lobby structure (bottom-northwest corner). */
    public static final BlockPos TUTORIAL_LOBBY_POS = new BlockPos(-60, -33, 0);

    /**
     * Room dimensions: X 0-19 (20 wide), Z 0-39 (40 long), Y 0-6 (7 tall).
     * Sections (Z offsets): Entry Hall 1-9 | divider 10 | Fire Practice 11-20
     *   | divider 21 | Extinguisher Types 22-30 | divider 31 | Earthquake Drill 32-39
     */

    /** Player spawn coordinates inside the tutorial lobby (entry hall centre). */
    public static final double TSPAWN_X = -50.5;
    public static final double TSPAWN_Y = -31.0;
    public static final double TSPAWN_Z =   4.5;

    /** PersistentData key used to identify BFP NPCs and store their role. */
    public static final String NPC_ROLE_TAG = "bfp_role";

    // NPC offsets from TUTORIAL_LOBBY_POS
    private static final BlockPos NPC_TRAINER_OFFSET        = new BlockPos(4, 2,  5);
    private static final BlockPos NPC_EXT_EXPERT_OFFSET     = new BlockPos(4, 2, 25);
    private static final BlockPos NPC_SAFETY_OFFICER_OFFSET = new BlockPos(4, 2, 35);

    // Bounded AABB covering all loaded chunks — avoids section-storage overflow from AABB.INFINITE
    private static final AABB WORLD_BOUNDS = new AABB(-32000, -512, -32000, 32000, 512, 32000);

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Places the tutorial lobby structure. Call from {@code ServerStartingEvent} so the
     * building exists before the first player login. NPCs are handled separately in
     * {@link #initNpcs(ServerLevel)} once entity storage is fully loaded.
     */
    public static void buildLobby(ServerLevel level) {
        boolean nbtLoaded = new SimulationStructureLoader(
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "bfp_tutorial_lobby"))
            .place(level, TUTORIAL_LOBBY_POS);

        if (!nbtLoaded) {
            BerongSMP.LOGGER.info("bfp_tutorial_lobby.nbt not found — building BFP lobby programmatically.");
            buildStructure(level, TUTORIAL_LOBBY_POS);
        }
        BerongSMP.LOGGER.info("BFP Tutorial Lobby structure placed at {}", TUTORIAL_LOBBY_POS);
    }

    /**
     * Clears any previously spawned instructor NPCs and spawns fresh ones.
     * Must be called from {@code ServerStartedEvent} (not {@code ServerStartingEvent})
     * so that entity chunk storage is loaded and old NPCs can actually be found and removed.
     */
    public static void initNpcs(ServerLevel level) {
        clearOldNpcs(level);
        spawnNpc(level, TUTORIAL_LOBBY_POS.offset(NPC_TRAINER_OFFSET),        NpcRole.TRAINER,        "§6Sgt. Reyes");
        spawnNpc(level, TUTORIAL_LOBBY_POS.offset(NPC_EXT_EXPERT_OFFSET),     NpcRole.EXT_EXPERT,     "§aOfficer Cruz");
        spawnNpc(level, TUTORIAL_LOBBY_POS.offset(NPC_SAFETY_OFFICER_OFFSET), NpcRole.SAFETY_OFFICER, "§cCapt. Santos");
        BerongSMP.LOGGER.info("BFP Tutorial Lobby NPCs spawned.");
    }

    /** @deprecated Use {@link #buildLobby} + {@link #initNpcs} separately. */
    @Deprecated
    public static void createTutorialLobby(ServerLevel level) {
        buildLobby(level);
        initNpcs(level);
    }

    // -----------------------------------------------------------------------
    // Structure building
    // -----------------------------------------------------------------------

    /**
     * Programmatically builds the BFP National Fire Training Center.
     *
     * <p>Room occupies offsets X 0-19, Y 0-6, Z 0-39 from {@code base}:
     * <ul>
     *   <li>Y=0 — stone-brick foundation</li>
     *   <li>Y=1 — floor surface (section-coloured)</li>
     *   <li>Y=2-5 — air interior / walls</li>
     *   <li>Y=6 — red-concrete ceiling with sea-lanterns</li>
     * </ul>
     * Sections: Entry Hall Z=1-9 | Fire Practice Z=11-20 | Extinguisher Types Z=22-30 | Quake Drill Z=32-39
     * Section dividers at Z=10, 21, 31 — passage X=9-11, Y=2-3.
     */
    private static void buildStructure(ServerLevel level, BlockPos base) {
        // Clear entire volume first
        for (int x = 0; x <= 19; x++)
            for (int y = 1; y <= 6; y++)
                for (int z = 0; z <= 39; z++)
                    level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);

        // Foundation
        fill(level, base, 0, 0, 0, 19, 0, 39, Blocks.STONE_BRICKS);

        // Ceiling — red concrete with sea-lanterns (2 columns per section)
        fill(level, base, 0, 6, 0, 19, 6, 39, Blocks.RED_CONCRETE);
        for (int lz : new int[]{4, 8, 15, 19, 25, 28, 34, 38}) {
            set(level, base,  5, 6, lz, Blocks.SEA_LANTERN);
            set(level, base, 14, 6, lz, Blocks.SEA_LANTERN);
        }

        // Outer walls — red concrete, Y=1-5
        fill(level, base,  0, 1,  0, 19, 5,  0, Blocks.RED_CONCRETE); // front
        fill(level, base,  0, 1, 39, 19, 5, 39, Blocks.RED_CONCRETE); // back
        fill(level, base,  0, 1,  0,  0, 5, 39, Blocks.RED_CONCRETE); // west
        fill(level, base, 19, 1,  0, 19, 5, 39, Blocks.RED_CONCRETE); // east

        // Default floor (polished andesite); sections override below
        fill(level, base, 1, 1, 1, 18, 1, 38, Blocks.POLISHED_ANDESITE);

        // Front entrance door — 4 wide (X=8-11), 3 tall (Y=2-4)
        for (int dx = 8; dx <= 11; dx++) {
            set(level, base, dx, 2, 0, Blocks.AIR);
            set(level, base, dx, 3, 0, Blocks.AIR);
            set(level, base, dx, 4, 0, Blocks.AIR);
        }
        // Yellow door frame + lintel
        for (int dy = 2; dy <= 4; dy++) {
            set(level, base, 7,  dy, 0, Blocks.YELLOW_CONCRETE);
            set(level, base, 12, dy, 0, Blocks.YELLOW_CONCRETE);
        }
        fill(level, base, 7, 5, 0, 12, 5, 0, Blocks.YELLOW_CONCRETE);

        // Side windows — 2 blocks tall at regular Z intervals
        for (int wz : new int[]{5, 15, 25, 35}) {
            set(level, base,  0, 3, wz, Blocks.GLASS);
            set(level, base,  0, 4, wz, Blocks.GLASS);
            set(level, base, 19, 3, wz, Blocks.GLASS);
            set(level, base, 19, 4, wz, Blocks.GLASS);
        }

        // Section dividers (white concrete; passage X=9-11, Y=2-3)
        sectionDivider(level, base, 10);
        sectionDivider(level, base, 21);
        sectionDivider(level, base, 31);

        // ── SECTION 1: Entry Hall (Z=1-9) ──────────────────────────────────
        // BFP emblem centred on floor (5×5 red + yellow cross)
        fill(level, base,  8, 1, 4, 12, 1, 8, Blocks.RED_CONCRETE);
        fill(level, base,  9, 1, 4, 11, 1, 8, Blocks.YELLOW_CONCRETE);
        fill(level, base,  8, 1, 5, 12, 1, 7, Blocks.YELLOW_CONCRETE);
        fill(level, base,  9, 1, 5, 11, 1, 7, Blocks.RED_CONCRETE); // centre cross back to red
        // Reception counter — Sgt. Reyes stands at NPC_TRAINER_OFFSET (X=4,Z=5)
        fill(level, base, 2, 2, 2, 7, 2, 2, Blocks.OAK_PLANKS);
        fill(level, base, 2, 3, 2, 7, 3, 2, Blocks.OAK_PLANKS);
        // Waiting benches on east side
        fill(level, base, 14, 2, 2, 17, 2, 8, Blocks.OAK_PLANKS);
        // BFP colour stripe on west wall
        for (int pz = 3; pz <= 8; pz++) {
            set(level, base, 0, 3, pz, pz % 2 == 0 ? Blocks.RED_CONCRETE   : Blocks.WHITE_CONCRETE);
            set(level, base, 0, 4, pz, pz % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.RED_CONCRETE);
        }

        // ── SECTION 2: Fire Practice Area (Z=11-20) ────────────────────────
        // Danger floor: yellow border, orange interior
        fill(level, base,  1, 1, 11, 18, 1, 20, Blocks.YELLOW_CONCRETE);
        fill(level, base,  2, 1, 12, 17, 1, 19, Blocks.ORANGE_CONCRETE);
        // Extinguisher rack on east wall
        fill(level, base, 18, 2, 13, 18, 5, 18, Blocks.RED_CONCRETE);
        // Warning chevrons on divider Z=10 — start at X=12 to keep the passage (X=9-11) clear
        for (int cx = 12; cx <= 18; cx++)
            set(level, base, cx, 2, 10, cx % 2 == 0 ? Blocks.YELLOW_CONCRETE : Blocks.RED_CONCRETE);

        // ── SECTION 3: Extinguisher Types Room (Z=22-30) ───────────────────
        fill(level, base, 1, 1, 22, 18, 1, 30, Blocks.LIGHT_BLUE_CONCRETE);
        // Instructor desk (Officer Cruz at X=4,Z=25)
        fill(level, base, 1, 2, 22, 6, 2, 22, Blocks.OAK_PLANKS);
        fill(level, base, 1, 3, 22, 6, 3, 22, Blocks.OAK_PLANKS);
        // Class A pedestal — green (ordinary combustibles) at X=3-4, Z=28
        fill(level, base, 3, 2, 28, 4, 2, 28, Blocks.STONE_BRICKS);
        fill(level, base, 3, 3, 28, 4, 4, 28, Blocks.GREEN_WOOL);
        fill(level, base, 3, 1, 28, 4, 1, 28, Blocks.GREEN_CONCRETE);
        // Class B pedestal — yellow (flammable liquids) at X=9-10, Z=28
        fill(level, base, 9, 2, 28, 10, 2, 28, Blocks.STONE_BRICKS);
        fill(level, base, 9, 3, 28, 10, 4, 28, Blocks.YELLOW_WOOL);
        fill(level, base, 9, 1, 28, 10, 1, 28, Blocks.YELLOW_CONCRETE);
        // Class C pedestal — blue (electrical) at X=15-16, Z=28
        fill(level, base, 15, 2, 28, 16, 2, 28, Blocks.STONE_BRICKS);
        fill(level, base, 15, 3, 28, 16, 4, 28, Blocks.BLUE_WOOL);
        fill(level, base, 15, 1, 28, 16, 1, 28, Blocks.BLUE_CONCRETE);

        // ── SECTION 4: Earthquake Drill Zone (Z=32-39) ─────────────────────
        fill(level, base, 1, 1, 32, 18, 1, 38, Blocks.GRAY_CONCRETE);
        // Cracked tiles scattered around
        for (int[] ct : new int[][]{{4,33},{10,35},{16,37},{7,36},{14,34}})
            set(level, base, ct[0], 1, ct[1], Blocks.CRACKED_STONE_BRICKS);
        // Large stone table/overhang for DROP-COVER-HOLD-ON drill
        fill(level, base, 5, 4, 34, 16, 4, 38, Blocks.STONE_BRICKS);
        // Corner and mid pillars holding the overhang
        for (int px : new int[]{5, 10, 16}) {
            set(level, base, px, 3, 34, Blocks.STONE_BRICK_WALL);
            set(level, base, px, 3, 38, Blocks.STONE_BRICK_WALL);
        }
        // Debris props
        set(level, base, 17, 2, 34, Blocks.COBBLESTONE);
        set(level, base, 18, 2, 36, Blocks.COBBLESTONE);
        set(level, base, 17, 2, 37, Blocks.STONE);
        set(level, base,  2, 2, 36, Blocks.COBBLESTONE);
        set(level, base,  3, 2, 37, Blocks.STONE);
        // Safety officer desk (Capt. Santos at X=4,Z=35)
        fill(level, base, 1, 2, 32, 6, 2, 32, Blocks.OAK_PLANKS);
        fill(level, base, 1, 3, 32, 6, 3, 32, Blocks.OAK_PLANKS);
        // BFP badge on back wall
        fill(level, base, 7, 2, 39, 12, 4, 39, Blocks.YELLOW_CONCRETE);
        fill(level, base, 9, 2, 39, 10, 4, 39, Blocks.RED_CONCRETE);
    }

    /** Places a section-divider wall at the given Z with a 3-block passage at X=9-11, Y=2-3. */
    private static void sectionDivider(ServerLevel level, BlockPos base, int z) {
        for (int x = 1; x <= 18; x++) {
            for (int y = 2; y <= 5; y++) {
                if ((x >= 9 && x <= 11) && (y == 2 || y == 3)) continue; // passage
                level.setBlock(base.offset(x, y, z), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);
            }
        }
    }

    private static void fill(ServerLevel level, BlockPos base,
                             int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        var state = block.defaultBlockState();
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    level.setBlock(base.offset(x, y, z), state, 3);
    }

    private static void set(ServerLevel level, BlockPos base, int x, int y, int z, Block block) {
        level.setBlock(base.offset(x, y, z), block.defaultBlockState(), 3);
    }

    // -----------------------------------------------------------------------
    // NPC management
    // -----------------------------------------------------------------------

    private static void clearOldNpcs(ServerLevel level) {
        level.getEntitiesOfClass(Villager.class, WORLD_BOUNDS,
            v -> v.getPersistentData().contains(NPC_ROLE_TAG))
            .forEach(v -> v.discard());
    }

    private static void spawnNpc(ServerLevel level, BlockPos pos, NpcRole role, String name) {
        Villager villager = new Villager(EntityType.VILLAGER, level);
        villager.setNoAi(true);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setPersistenceRequired();
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(true);
        villager.getPersistentData().putString(NPC_ROLE_TAG, role.name());
        villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(villager);
    }
}
