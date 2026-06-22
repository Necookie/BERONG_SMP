package net.necookie.disastersim.world;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Defines emergency-exit zones for the LSPU Library simulation arena.
 *
 * All zones are PLACEHOLDER — walk each door with F3 in-game and update the
 * min/max coordinates. Labels match the map_metadata.json exit entries.
 *
 * A zone is considered "passed" when the player's position is inside the AABB.
 * Thin slabs (≤2 blocks deep in Z) work because AABB.contains() uses strict
 * inequality on the max boundary, so players stepping through the door cross the
 * min boundary first.
 */
public class ExitZones {

    public record ExitZone(String label, AABB bounds) {
        public boolean contains(Vec3 pos) { return bounds.contains(pos); }
    }

    /**
     * All named emergency exits for the LSPU Library.
     * IMPORTANT: coordinates below are PLACEHOLDER — tune after runServer.
     * SIM_POS = (30, -34, 83); main front door faces Z-negative (south-facing structure).
     */
    public static final List<ExitZone> ZONES = List.of(
        new ExitZone("main_exit",  new AABB(38, -34, 81, 46, -31, 83)),
        new ExitZone("side_exit",  new AABB(28, -34, 86, 32, -31, 90)),
        new ExitZone("rear_exit",  new AABB(40, -34, 107, 48, -31, 109))
    );

    /**
     * Returns the first ExitZone that contains the given position, or null.
     */
    public static ExitZone find(Vec3 pos) {
        for (ExitZone z : ZONES) {
            if (z.contains(pos)) return z;
        }
        return null;
    }
}
