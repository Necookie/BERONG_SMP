package net.necookie.disastersim.world;

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
 * <p>Room layout (offsets from {@link #TUTORIAL_LOBBY_POS}):
 * <pre>
 *   Z  0     : Front entrance wall (door at X=7-8)
 *   Z  1- 7  : Entry hall — Sgt. Reyes (TRAINER) at X=5, Z=3
 *   Z  8     : Section divider
 *   Z  9-13  : Fire practice area (practice fires at X=10-12, Z=8-10)
 *   Z 14     : Section divider
 *   Z 15-18  : Extinguisher types — Officer Cruz (EXT_EXPERT) at X=5, Z=15
 *   Z 19     : Section divider
 *   Z 20-27  : Earthquake drill zone — Capt. Santos (SAFETY_OFFICER) at X=5, Z=21
 * </pre>
 */
public class TutorialLobbyManager {

    /** World origin of the tutorial lobby structure (bottom-northwest corner). */
    public static final BlockPos TUTORIAL_LOBBY_POS = new BlockPos(-60, -33, 0);

    /** Player spawn coordinates inside the tutorial lobby. */
    public static final double TSPAWN_X = -52.5;
    public static final double TSPAWN_Y = -31.0;
    public static final double TSPAWN_Z =   3.5;

    /** PersistentData key used to identify BFP NPCs and store their role. */
    public static final String NPC_ROLE_TAG = "bfp_role";

    // NPC offsets from TUTORIAL_LOBBY_POS
    private static final BlockPos NPC_TRAINER_OFFSET        = new BlockPos(5, 2,  3);
    private static final BlockPos NPC_EXT_EXPERT_OFFSET     = new BlockPos(5, 2, 15);
    private static final BlockPos NPC_SAFETY_OFFICER_OFFSET = new BlockPos(5, 2, 21);

    // Bounded AABB covering all loaded chunks — avoids section-storage overflow from AABB.INFINITE
    private static final AABB WORLD_BOUNDS = new AABB(-32000, -512, -32000, 32000, 512, 32000);

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Creates the tutorial lobby. Tries to load {@code bfp_tutorial_lobby.nbt}; if missing,
     * builds the room programmatically. Always clears and re-spawns the three instructor NPCs.
     */
    public static void createTutorialLobby(ServerLevel level) {
        boolean nbtLoaded = new SimulationStructureLoader(
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "bfp_tutorial_lobby"))
            .place(level, TUTORIAL_LOBBY_POS);

        if (!nbtLoaded) {
            BerongSMP.LOGGER.info("bfp_tutorial_lobby.nbt not found — building BFP lobby programmatically.");
            buildStructure(level, TUTORIAL_LOBBY_POS);
        }

        clearOldNpcs(level);
        spawnNpc(level, TUTORIAL_LOBBY_POS.offset(NPC_TRAINER_OFFSET),        NpcRole.TRAINER,        "§6Sgt. Reyes");
        spawnNpc(level, TUTORIAL_LOBBY_POS.offset(NPC_EXT_EXPERT_OFFSET),     NpcRole.EXT_EXPERT,     "§aOfficer Cruz");
        spawnNpc(level, TUTORIAL_LOBBY_POS.offset(NPC_SAFETY_OFFICER_OFFSET), NpcRole.SAFETY_OFFICER, "§cCapt. Santos");

        BerongSMP.LOGGER.info("BFP Tutorial Lobby initialised at {}", TUTORIAL_LOBBY_POS);
    }

    /** Returns {@code true} if the player has not yet completed the tutorial. */
    public static boolean needsTutorialLobby(UUID uuid) {
        return !TutorialManager.isComplete(uuid);
    }

    // -----------------------------------------------------------------------
    // Structure building
    // -----------------------------------------------------------------------

    /**
     * Programmatically builds the BFP National Fire Training Center.
     *
     * <p>Room occupies offsets X 0-15, Y 0-5, Z 0-27 from {@code base}:
     * <ul>
     *   <li>Y=0 — stone-brick foundation</li>
     *   <li>Y=1 — floor surface (section-coloured)</li>
     *   <li>Y=2-4 — air (interior / walls)</li>
     *   <li>Y=5 — red-concrete ceiling with embedded sea-lanterns</li>
     * </ul>
     */
    private static void buildStructure(ServerLevel level, BlockPos base) {
        // Clear room volume (keep Y=0 foundation intact below)
        for (int x = 0; x <= 15; x++)
            for (int y = 1; y <= 5; y++)
                for (int z = 0; z <= 27; z++)
                    level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);

        // Foundation
        fill(level, base, 0, 0, 0, 15, 0, 27, Blocks.STONE_BRICKS);

        // Ceiling — red concrete (BFP brand colour) with embedded sea-lanterns for lighting
        fill(level, base, 0, 5, 0, 15, 5, 27, Blocks.RED_CONCRETE);
        set(level, base, 7, 5,  3, Blocks.SEA_LANTERN);
        set(level, base, 3, 5, 11, Blocks.SEA_LANTERN);
        set(level, base, 11, 5, 11, Blocks.SEA_LANTERN);
        set(level, base, 7, 5, 16, Blocks.SEA_LANTERN);
        set(level, base, 5, 5, 23, Blocks.SEA_LANTERN);
        set(level, base, 11, 5, 23, Blocks.SEA_LANTERN);

        // Outer walls — red concrete
        fill(level, base,  0, 1,  0, 15, 4,  0, Blocks.RED_CONCRETE); // front (Z=0)
        fill(level, base,  0, 1, 27, 15, 4, 27, Blocks.RED_CONCRETE); // back  (Z=27)
        fill(level, base,  0, 1,  0,  0, 4, 27, Blocks.RED_CONCRETE); // west  (X=0)
        fill(level, base, 15, 1,  0, 15, 4, 27, Blocks.RED_CONCRETE); // east  (X=15)

        // Default floor (polished andesite); sections override below
        fill(level, base, 1, 1, 1, 14, 1, 26, Blocks.POLISHED_ANDESITE);

        // Door opening at front wall X=7-8, Y=2-3
        set(level, base, 7, 2, 0, Blocks.AIR); set(level, base, 7, 3, 0, Blocks.AIR);
        set(level, base, 8, 2, 0, Blocks.AIR); set(level, base, 8, 3, 0, Blocks.AIR);
        // Yellow door frame
        set(level, base, 6, 2, 0, Blocks.YELLOW_CONCRETE); set(level, base, 6, 3, 0, Blocks.YELLOW_CONCRETE);
        set(level, base, 9, 2, 0, Blocks.YELLOW_CONCRETE); set(level, base, 9, 3, 0, Blocks.YELLOW_CONCRETE);
        fill(level, base, 6, 4, 0, 9, 4, 0, Blocks.YELLOW_CONCRETE); // lintel

        // Side windows (plain glass)
        for (int wz : new int[]{4, 11, 16, 22}) {
            set(level, base,  0, 3, wz, Blocks.GLASS);
            set(level, base, 15, 3, wz, Blocks.GLASS);
        }

        // Section dividers (white concrete, passage at X=5-6, Y=2-3)
        sectionDivider(level, base,  8);
        sectionDivider(level, base, 14);
        sectionDivider(level, base, 19);

        // ── SECTION 1: Entry Hall (Z=1-7) ──────────────────────────────────
        // BFP emblem on floor (red base, yellow centre cross)
        fill(level, base, 6, 1, 3,  9, 1,  6, Blocks.RED_CONCRETE);
        fill(level, base, 7, 1, 3,  8, 1,  6, Blocks.YELLOW_CONCRETE);
        fill(level, base, 6, 1, 4,  9, 1,  5, Blocks.YELLOW_CONCRETE);
        set(level, base, 7, 1, 4, Blocks.RED_CONCRETE); set(level, base, 8, 1, 4, Blocks.RED_CONCRETE);
        set(level, base, 7, 1, 5, Blocks.RED_CONCRETE); set(level, base, 8, 1, 5, Blocks.RED_CONCRETE);
        // Reception counter (trainer stands behind it)
        fill(level, base, 3, 2, 1, 7, 2, 1, Blocks.OAK_PLANKS);
        fill(level, base, 3, 3, 1, 7, 3, 1, Blocks.OAK_PLANKS);
        // Waiting bench on east side
        fill(level, base, 12, 2, 2, 14, 2, 6, Blocks.OAK_PLANKS);
        // Poster on west wall: red/white alternating blocks
        set(level, base, 0, 2, 3, Blocks.RED_CONCRETE);  set(level, base, 0, 3, 3, Blocks.WHITE_CONCRETE);
        set(level, base, 0, 2, 4, Blocks.WHITE_CONCRETE); set(level, base, 0, 3, 4, Blocks.RED_CONCRETE);
        set(level, base, 0, 2, 5, Blocks.RED_CONCRETE);  set(level, base, 0, 3, 5, Blocks.WHITE_CONCRETE);

        // ── SECTION 2: Fire Practice Area (Z=9-13) ─────────────────────────
        // Danger floor: orange with yellow border, red interior
        fill(level, base,  1, 1,  9, 14, 1,  9, Blocks.YELLOW_CONCRETE);
        fill(level, base,  1, 1, 13, 14, 1, 13, Blocks.YELLOW_CONCRETE);
        fill(level, base,  1, 1,  9,  1, 1, 13, Blocks.YELLOW_CONCRETE);
        fill(level, base, 14, 1,  9, 14, 1, 13, Blocks.YELLOW_CONCRETE);
        fill(level, base,  2, 1, 10, 13, 1, 12, Blocks.ORANGE_CONCRETE);
        // Extinguisher rack on east wall
        fill(level, base, 14, 2, 10, 14, 3, 12, Blocks.RED_CONCRETE);
        // Warning chevrons on back of divider Z=8 (visible from fire side)
        set(level, base,  9, 2, 8, Blocks.YELLOW_CONCRETE); set(level, base, 10, 2, 8, Blocks.RED_CONCRETE);
        set(level, base, 11, 2, 8, Blocks.YELLOW_CONCRETE); set(level, base, 12, 2, 8, Blocks.RED_CONCRETE);
        set(level, base, 13, 2, 8, Blocks.YELLOW_CONCRETE);

        // ── SECTION 3: Extinguisher Types (Z=15-18) ────────────────────────
        fill(level, base, 1, 1, 15, 14, 1, 18, Blocks.LIGHT_BLUE_CONCRETE);
        // Instructor desk — kept to X=1-4 so it never blocks the passage at X=5-6
        fill(level, base, 1, 2, 15, 4, 2, 15, Blocks.OAK_PLANKS);
        // Class A display — green (ordinary combustibles)
        set(level, base, 2, 2, 17, Blocks.STONE_BRICKS); set(level, base, 3, 2, 17, Blocks.STONE_BRICKS);
        set(level, base, 2, 3, 17, Blocks.GREEN_WOOL);   set(level, base, 3, 3, 17, Blocks.GREEN_WOOL);
        // Class B display — yellow (flammable liquids)
        set(level, base, 7, 2, 17, Blocks.STONE_BRICKS); set(level, base, 8, 2, 17, Blocks.STONE_BRICKS);
        set(level, base, 7, 3, 17, Blocks.YELLOW_WOOL);  set(level, base, 8, 3, 17, Blocks.YELLOW_WOOL);
        // Class C display — blue (electrical)
        set(level, base, 12, 2, 17, Blocks.STONE_BRICKS); set(level, base, 13, 2, 17, Blocks.STONE_BRICKS);
        set(level, base, 12, 3, 17, Blocks.BLUE_WOOL);    set(level, base, 13, 3, 17, Blocks.BLUE_WOOL);
        // A/B/C labels below pedestals (coloured floor)
        fill(level, base, 2, 1, 17, 3, 1, 17, Blocks.GREEN_CONCRETE);
        fill(level, base, 7, 1, 17, 8, 1, 17, Blocks.YELLOW_CONCRETE);
        fill(level, base, 12, 1, 17, 13, 1, 17, Blocks.BLUE_CONCRETE);

        // ── SECTION 4: Earthquake Drill Zone (Z=20-27) ─────────────────────
        fill(level, base, 1, 1, 20, 14, 1, 26, Blocks.GRAY_CONCRETE);
        // Cracked tiles — simulates earthquake damage
        set(level, base,  3, 1, 21, Blocks.CRACKED_STONE_BRICKS);
        set(level, base,  9, 1, 23, Blocks.CRACKED_STONE_BRICKS);
        set(level, base, 13, 1, 25, Blocks.CRACKED_STONE_BRICKS);
        // Stone overhang for COVER — players must crouch under it (Y=4 slab, Y=3 pillars)
        fill(level, base, 3, 4, 22, 10, 4, 25, Blocks.STONE_BRICKS);
        set(level, base,  3, 3, 22, Blocks.STONE_BRICK_WALL);
        set(level, base, 10, 3, 22, Blocks.STONE_BRICK_WALL);
        set(level, base,  3, 3, 25, Blocks.STONE_BRICK_WALL);
        set(level, base, 10, 3, 25, Blocks.STONE_BRICK_WALL);
        // Debris props
        set(level, base, 12, 2, 22, Blocks.COBBLESTONE);
        set(level, base, 13, 2, 24, Blocks.COBBLESTONE);
        set(level, base, 12, 2, 25, Blocks.STONE);
        // Safety officer desk — kept to X=1-4 so it never blocks the passage at X=5-6
        fill(level, base, 1, 2, 20, 4, 2, 20, Blocks.OAK_PLANKS);
        // BFP badge on back wall
        fill(level, base, 6, 2, 27, 9, 3, 27, Blocks.YELLOW_CONCRETE);
        set(level, base, 7, 2, 27, Blocks.RED_CONCRETE); set(level, base, 8, 2, 27, Blocks.RED_CONCRETE);
        set(level, base, 7, 3, 27, Blocks.RED_CONCRETE); set(level, base, 8, 3, 27, Blocks.RED_CONCRETE);
    }

    /** Places a section-divider wall at the given Z with a 2-block passage at X=5-6, Y=2-3. */
    private static void sectionDivider(ServerLevel level, BlockPos base, int z) {
        for (int x = 1; x <= 14; x++) {
            for (int y = 2; y <= 4; y++) {
                if ((x == 5 || x == 6) && (y == 2 || y == 3)) continue; // passage
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
