package net.necookie.disastersim.common.zones;

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
     * Emergency exits for the LSPU Library.
     * SIM_POS = (30, -34, 83); main front door faces Z-negative.
     */
    public static final List<ExitZone> ZONES = List.of(
        new ExitZone("main_exit", new AABB(50, -34, 93, 54, -30, 96))
        // side_exit and rear_exit — add when tuned with F3 in-game
    );

    /**
     * Emergency exits for the CCS Admin Building.
     * IMPORTANT: all coordinates below are PLACEHOLDER — tune with F3 in-game.
     * See docs/f3_tuning_todo.md for the full checklist.
     * CCS_POS = (76, -34, 4); building spans X: 76–136, Z: 4–72.
     */
    public static final List<ExitZone> CCS_ZONES = List.of(
        // PLACEHOLDER — centre of south wall (X:95–125, Z:68–74). Verify with F3.
        new ExitZone("ccs_main_exit", new AABB(95, -33, 68, 125, -29, 74))
        // ccs_side_exit — add when tuned with F3 in-game
    );

    /**
     * Emergency exits for New Sim Building 2.0 — STALE (2026-07-14): originally derived as the two
     * thresholds where "Main Hallway" (X -95..-91, Z 446..530) meets the "Lobby" room (X -118..-81,
     * Z 482..498), back when the assembly zone was itself approximated as that Lobby room. Now that
     * {@link AssemblyZone#getNewSim2Zone()} has been replaced with the real F3-verified assembly
     * area at X -164..-148, Z 466..512 (genuine open ground west of the building, not the Lobby),
     * these doorway coordinates no longer point toward the actual assembly area and are known-wrong
     * — kept as a placeholder only so {@code emergency_exit} still fires *something* rather than
     * nothing. The real west-facing exit door(s) leading from the building out to the new assembly
     * area need an in-game F3 walk-through — see docs/f3_tuning_todo.md §7 (updated).
     */
    public static final List<ExitZone> NEW_SIM2_ZONES = List.of(
        new ExitZone("new_sim2_lobby_south_door", new AABB(-96, -33, 481, -90, -29, 483)),
        new ExitZone("new_sim2_lobby_north_door", new AABB(-96, -33, 497, -90, -29, 499))
    );

    /**
     * Returns the first ExitZone that contains the given position, or null.
     * @param isCCS true to search CCS zones, false for LSPU Library zones.
     */
    public static ExitZone find(Vec3 pos, boolean isCCS) {
        List<ExitZone> list = isCCS ? CCS_ZONES : ZONES;
        for (ExitZone z : list) {
            if (z.contains(pos)) return z;
        }
        return null;
    }

    /** New Sim Building 2.0's own lookup — a 3rd building doesn't fit the existing boolean isCCS shape. */
    public static ExitZone findNewSim2(Vec3 pos) {
        for (ExitZone z : NEW_SIM2_ZONES) {
            if (z.contains(pos)) return z;
        }
        return null;
    }
}
