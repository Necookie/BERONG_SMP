package net.necookie.disastersim.common.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Maps a player's block position (relative to SIM_POS) to a named room inside the LSPU library.
 * Bounding boxes are approximate — tune against the actual NBT after running the server.
 */
public enum SimRoom {
    // LSPU Library rooms (offsets from SIM_POS = 30,-34,83)
    COMPUTER_LAB,
    MAIN_HALL,
    ENTRANCE,
    STAIRWELL,
    UPPER_FLOOR,
    // CCS Admin Building floors (offsets from CCS_POS = 76,-34,4)
    CCS_GROUND_FLOOR,
    CCS_UPPER_FLOOR,
    // New Sim Building 2.0 floors (absolute world coords; building placed at -182,-34,358)
    NEW_SIM2_GROUND_FLOOR,
    NEW_SIM2_UPPER_FLOOR,
    OUTSIDE;

    /** A named room on the CCS 2nd floor with its absolute world-space AABB. */
    public record CcsRoom(String name, AABB bounds) {}

    /**
     * Named rooms on the CCS Admin Building 2nd floor (absolute world coords).
     * Floor Y = -25, ceiling Y = -22 (3-block-tall rooms). Coords verified with F3.
     */
    public static final List<CcsRoom> CCS_UPPER_ROOMS = List.of(
        new CcsRoom("CCS Mini Library", new AABB( 94, -25,  6,  99, -22, 11)),
        new CcsRoom("Room 202",         new AABB(101, -25,  6, 105, -22, 11)),
        new CcsRoom("Room 203",         new AABB(107, -25,  6, 112, -22, 11)),
        new CcsRoom("Room 204",         new AABB(114, -25,  6, 119, -22, 11)),
        new CcsRoom("Room 205",         new AABB(121, -25,  6, 126, -22, 11)),
        new CcsRoom("TESOL",            new AABB(130, -25, 17, 136, -22, 22)),
        new CcsRoom("Computer Lab",     new AABB(130, -25, 24, 136, -22, 31)),
        new CcsRoom("MacLab",           new AABB(130, -25, 33, 136, -22, 39)),
        new CcsRoom("Room 207",         new AABB(132, -25, 41, 136, -22, 49))
    );

    /**
     * Named rooms on the CCS Admin Building 1st floor (absolute world coords).
     * Floor Y = -32, ceiling Y = -29 (3-block-tall rooms). Coords verified with F3.
     */
    public static final List<CcsRoom> CCS_GROUND_ROOMS = List.of(
        new CcsRoom("Room 105",      new AABB( 94, -32,  6,  99, -29, 11)),
        new CcsRoom("Room 106",      new AABB(101, -32,  6, 105, -29, 11)),
        new CcsRoom("Room 107",      new AABB(107, -32,  6, 112, -29, 11)),
        new CcsRoom("Dean's Office", new AABB(114, -32,  6, 119, -29, 11)),
        new CcsRoom("Faculty Room",  new AABB(121, -32,  6, 126, -29, 11)),
        new CcsRoom("ICTS",          new AABB(130, -32, 17, 136, -29, 26)),
        new CcsRoom("ICTS 2",        new AABB(131, -32, 28, 136, -29, 31))
    );

    /**
     * Named rooms in New Sim Building 2.0's 2nd floor (absolute world coords, F3-surveyed —
     * see docs/new_sim_building2_rooms.md). Floor Y = -24, ceiling Y = -14 (11 blocks tall).
     * This is the pool {@code SimulationManager.findRandomSpawnInNewSim2Upper} draws from.
     *
     * <p><b>2026-07-14 recalibration:</b> in-game F3 re-observation found the 1st floor reading
     * too high and the 2nd floor reading too low against the real structure. Floor Y dropped 1
     * block (-23 → -24); ceiling Y is unchanged (the room gained a block of height rather than
     * shifting as a whole) — see {@link #NEW_SIM2_GROUND_ROOMS}'s matching note for the 1st floor.
     */
    public static final List<CcsRoom> NEW_SIM2_UPPER_ROOMS = List.of(
        new CcsRoom("Room 201",         new AABB( -88, -24, 446,  -81, -14, 453)),
        new CcsRoom("Male CR",          new AABB(-100, -24, 446,  -93, -14, 453)),
        new CcsRoom("Female CR",        new AABB(-100, -24, 455,  -93, -14, 462)),
        new CcsRoom("Conference Room",  new AABB( -88, -24, 455,  -81, -14, 471)),
        new CcsRoom("Room 202",         new AABB(-100, -24, 464,  -93, -14, 471)),
        new CcsRoom("Room 203",         new AABB( -88, -24, 473,  -81, -14, 480)),
        new CcsRoom("Lecture Hall",     new AABB( -88, -24, 482,  -81, -14, 498)),
        new CcsRoom("ComLab 201",       new AABB(-100, -24, 473,  -93, -14, 489)),
        new CcsRoom("Room 204",         new AABB(-100, -24, 491,  -93, -14, 498)),
        new CcsRoom("Room 205",         new AABB(-100, -24, 500,  -93, -14, 507)),
        new CcsRoom("Room 206",         new AABB( -88, -24, 500,  -81, -14, 507)),
        new CcsRoom("Room 207",         new AABB( -88, -24, 509,  -81, -14, 516)),
        new CcsRoom("Clinic",           new AABB( -88, -24, 518,  -81, -14, 525)),
        new CcsRoom("Study Lobby",      new AABB(-100, -24, 509,  -93, -14, 525)),
        new CcsRoom("Faculty Room",     new AABB( -88, -24, 527,  -81, -14, 534)),
        new CcsRoom("Research Lab",     new AABB(-100, -24, 527,  -93, -14, 534)),
        new CcsRoom("Library",          new AABB(-101, -24, 536,  -81, -14, 542)),
        new CcsRoom("Basketball Court", new AABB(-105, -24, 434,  -81, -14, 444)),
        new CcsRoom("Hallway 1",        new AABB(-105, -24, 445, -102, -14, 542)),
        new CcsRoom("Hallway 2",        new AABB( -91, -24, 445,  -90, -14, 535))
    );

    /**
     * Named rooms in New Sim Building 2.0's 1st floor (absolute world coords, F3-surveyed —
     * see docs/new_sim_building2_rooms.md). Most rooms are Y -34..-25 (10 tall); "Main Hallway",
     * "Lobby", and "General CR" were originally captured as thinner slices — all bumped to a full
     * -34..-25 standing volume here so {@code AABB.contains()} (strict on the max boundary)
     * actually admits a standing player, per the survey doc's own note.
     *
     * <p><b>2026-07-14 recalibration:</b> in-game F3 re-observation found this floor reading too
     * high against the real structure — both floor Y (-33 → -34) and ceiling Y (-24 → -25) dropped
     * 1 block, shifting the whole floor down while keeping its height. See
     * {@link #NEW_SIM2_UPPER_ROOMS}'s matching note for the 2nd floor.
     */
    public static final List<CcsRoom> NEW_SIM2_GROUND_ROOMS = List.of(
        new CcsRoom("Cafeteria",          new AABB(-105, -34, 434,  -81, -25, 444)),
        new CcsRoom("Room 101",           new AABB( -89, -34, 446,  -81, -25, 456)),
        new CcsRoom("Under Maintenance",  new AABB(-105, -34, 446,  -97, -25, 456)),
        new CcsRoom("Room 102",           new AABB( -89, -34, 458,  -81, -25, 468)),
        new CcsRoom("Room 103",           new AABB(-105, -34, 458,  -97, -25, 468)),
        new CcsRoom("Main Hallway",       new AABB( -95, -34, 446,  -91, -25, 530)),
        new CcsRoom("Lobby",              new AABB(-118, -34, 482,  -81, -25, 498)),
        new CcsRoom("Room 104",           new AABB( -89, -34, 496,  -81, -25, 506)),
        new CcsRoom("Kitchen Lobby",      new AABB(-105, -34, 508,  -97, -25, 524)),
        new CcsRoom("Room 105",           new AABB( -89, -34, 508,  -81, -25, 518)),
        new CcsRoom("Room 106",           new AABB( -89, -34, 520,  -81, -25, 530)),
        new CcsRoom("Principal's Office", new AABB(-105, -34, 526,  -97, -25, 530)),
        new CcsRoom("Badminton Court",    new AABB(-105, -34, 532,  -81, -25, 542)),
        new CcsRoom("General CR",         new AABB(-118, -34, 472, -106, -25, 476))
    );

    // Offsets relative to SIM_POS = (30, -34, 83). Y=0 is the ground floor.
    // Placeholder values — tune after walking the structure in-game.
    private static final Object[][] BOUNDS_DATA = {
        { COMPUTER_LAB,  0,  0,  0, 12,  5, 10 },
        { MAIN_HALL,     0,  0, 10, 20,  5, 25 },
        { ENTRANCE,      4,  0, 22, 10,  5, 28 },
        { STAIRWELL,    18,  0,  5, 22, 10, 12 },
        { UPPER_FLOOR,   0,  5,  0, 20, 10, 25 },
    };

    // CCS Admin Building at world pos (76, -34, 4), 0 CCW rotations, ~55×24×68 blocks.
    // Y offset 0 = y=-34 (building base). Ground floor ~y=-32 to -26, upper ~y=-26 to -18.
    private static final BlockPos CCS_ORIGIN = new BlockPos(76, -34, 4);
    private static final Object[][] CCS_BOUNDS_DATA = {
        { CCS_GROUND_FLOOR,  0,  0,  0, 60,  8, 68 },
        { CCS_UPPER_FLOOR,   0,  8,  0, 60, 22, 68 },
    };

    public static SimRoom fromPos(BlockPos playerPos, BlockPos simOrigin) {
        Vec3 rel = new Vec3(
            playerPos.getX() - simOrigin.getX(),
            playerPos.getY() - simOrigin.getY(),
            playerPos.getZ() - simOrigin.getZ()
        );
        for (Object[] row : BOUNDS_DATA) {
            SimRoom room = (SimRoom) row[0];
            AABB box = new AABB(
                (int)row[1], (int)row[2], (int)row[3],
                (int)row[4], (int)row[5], (int)row[6]
            );
            if (box.contains(rel)) return room;
        }
        return OUTSIDE;
    }

    public static SimRoom fromCCSPos(BlockPos playerPos) {
        Vec3 rel = new Vec3(
            playerPos.getX() - CCS_ORIGIN.getX(),
            playerPos.getY() - CCS_ORIGIN.getY(),
            playerPos.getZ() - CCS_ORIGIN.getZ()
        );
        for (Object[] row : CCS_BOUNDS_DATA) {
            SimRoom room = (SimRoom) row[0];
            AABB box = new AABB(
                (int)row[1], (int)row[2], (int)row[3],
                (int)row[4], (int)row[5], (int)row[6]
            );
            if (box.contains(rel)) return room;
        }
        return OUTSIDE;
    }

    // New Sim Building 2.0 whole-building envelope, split by floor (absolute world coords,
    // padded slightly beyond the surveyed room list so a player anywhere in the building still
    // resolves to a floor rather than OUTSIDE — see docs/new_sim_building2_rooms.md).
    // 2026-07-14: shifted 1 block to match NEW_SIM2_GROUND_ROOMS/NEW_SIM2_UPPER_ROOMS's recalibration.
    private static final AABB NEW_SIM2_GROUND_BOUNDS = new AABB(-119, -35, 433, -80, -24, 543);
    private static final AABB NEW_SIM2_UPPER_BOUNDS  = new AABB(-119, -24, 433, -80, -13, 543);

    /** Coarse floor bucket (not a named room) — matches {@link #fromCCSPos}'s resolution, used for telemetry. */
    public static SimRoom fromNewSim2Pos(BlockPos playerPos) {
        Vec3 v = Vec3.atCenterOf(playerPos);
        if (NEW_SIM2_GROUND_BOUNDS.contains(v)) return NEW_SIM2_GROUND_FLOOR;
        if (NEW_SIM2_UPPER_BOUNDS.contains(v))  return NEW_SIM2_UPPER_FLOOR;
        return OUTSIDE;
    }

    /** Named-room lookup (34-room resolution) — used by the {@code /sim_scan_hazards} verification tool. */
    public static String nameInNewSim2(BlockPos pos) {
        Vec3 v = Vec3.atCenterOf(pos);
        for (CcsRoom room : NEW_SIM2_UPPER_ROOMS) if (room.bounds().contains(v)) return room.name();
        for (CcsRoom room : NEW_SIM2_GROUND_ROOMS) if (room.bounds().contains(v)) return room.name();
        return "(unnamed area)";
    }
}
