package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.necookie.disastersim.BerongSMP;

/**
 * Places {@code new_tut_building1.0.schem} — a WorldEdit {@code //copy -e} capture of the
 * redesigned tutorial building, baked-in NPCs ({@code berongsmp:custom_npc}), gear-display
 * armor stands, item frames and a painting all included — at its fixed world position.
 *
 * <p>Unlike {@link TutorialLobbyManager} (which builds structure and NPCs in two separate passes,
 * since its NPCs are spawned by hardcoded offset rather than captured in the structure file),
 * this building's entities are already part of the schematic. {@link SchemLoader#place} spawns
 * them inline, so {@link #place} must run from {@code ServerStartedEvent} once entity chunk
 * storage is fully loaded — the same reason {@code TutorialLobbyManager.initNpcs} isn't called
 * from {@code ServerStartingEvent} either. Calling this any earlier risks the freshly-spawned
 * NPCs colliding with the same-UUID copies the previous server run persisted to disk, once that
 * chunk's entity data finishes loading.
 */
public final class NewTutBuildingManager {

    /** Exact world position requested for the schematic's minimum (0,0,0) corner. */
    public static final BlockPos POS = new BlockPos(-177, -34, 8);

    private static final String SCHEM_PATH = "structure/new_tut_building1.0.schem";

    private NewTutBuildingManager() {}

    /** Call once from {@code ServerStartedEvent}. */
    public static void place(ServerLevel level) {
        boolean placed = new SchemLoader(Identifier.fromNamespaceAndPath(BerongSMP.MODID, SCHEM_PATH))
                .place(level, POS);
        if (placed) {
            BerongSMP.LOGGER.info("New tutorial building placed at {}", POS);
        } else {
            BerongSMP.LOGGER.error("Failed to place new tutorial building ({}) at {}", SCHEM_PATH, POS);
        }
    }
}
