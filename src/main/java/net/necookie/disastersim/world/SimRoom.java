package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
    OUTSIDE;

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
}
