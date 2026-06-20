package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Maps a player's block position (relative to SIM_POS) to a named room inside the LSPU library.
 * Bounding boxes are approximate — tune against the actual NBT after running the server.
 */
public enum SimRoom {
    COMPUTER_LAB,
    MAIN_HALL,
    ENTRANCE,
    STAIRWELL,
    UPPER_FLOOR,
    OUTSIDE;

    // Offsets relative to SIM_POS = (30, -34, 83). Y=0 is the ground floor.
    // These are rough placeholder values — adjust after walking the structure in-game.
    private static final Object[][] BOUNDS_DATA = {
        { COMPUTER_LAB,  0,  0,  0, 12,  5, 10 },
        { MAIN_HALL,     0,  0, 10, 20,  5, 25 },
        { ENTRANCE,      4,  0, 22, 10,  5, 28 },
        { STAIRWELL,    18,  0,  5, 22, 10, 12 },
        { UPPER_FLOOR,   0,  5,  0, 20, 10, 25 },
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
}
