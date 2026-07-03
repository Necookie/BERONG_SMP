package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.entity.NpcType;

import java.util.List;
import java.util.Map;

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

    /**
     * Old tunnel-finish handoff spot from the two-NPC Cruz design (pre-Academy-polish-pass) — the
     * schematic still bakes in a second {@code OFFICER_CRUZ} here. The Academy now routes all of
     * Room 1's dialogue through a single escorting Cruz, so this duplicate is discarded right after
     * every placement. This is a permanent fixup, not a one-time migration: {@link SchemLoader}
     * discards and respawns every schematic entity fresh on every server start, so without this the
     * duplicate would silently reappear on each boot.
     */
    private static final AABB DUPLICATE_CRUZ_BOUNDS = new AABB(-123.5, -33, 49.5, -123.5, -33, 49.5).inflate(2);

    /** A named F3-captured reference viewpoint inside the building, for {@code /bfp new_tutorial}. */
    public record Viewpoint(double x, double y, double z, float yaw, float pitch) {}

    /**
     * Reference viewpoints for {@code /bfp new_tutorial <name>} (see
     * {@code BfpAdminCommands.newTutorialCommand}) — one admin teleport target per named station,
     * captured via in-game F3 while standing at that spot. Add more entries here as additional
     * stations (fire practice, earthquake drill, ...) get captured; each key becomes its own
     * subcommand automatically.
     *
     * <p>{@code officer_cruz}: F3 reading X=-148.570 Y=-33.00000 Z=32.248, facing west (yaw
     * 88.3°) — the point in the Extinguisher Types section facing Officer Cruz's NPC.
     *
     * <p>{@code sgt_reyes}/{@code sgt_santos}/{@code capt_morfe} aren't F3-captured "facing"
     * shots yet (unlike {@code officer_cruz}) — they're just each NPC's own anchor position (see
     * {@code academy.room1/2/3/4} for where these same coordinates are used in the actual room
     * logic), with yaw/pitch left at 0 as a placeholder until a real facing angle is captured.
     */
    public static final Map<String, Viewpoint> VIEWPOINTS = Map.of(
            "officer_cruz", new Viewpoint(-148.570, -33.0, 32.248, 88.3f, 0f),
            "sgt_reyes", new Viewpoint(-172.5, -33.0, 17.5, 0f, 0f),
            "sgt_santos", new Viewpoint(-170.5, -33.0, 33.5, 0f, 0f),
            "capt_morfe", new Viewpoint(-108.5, -33.0, 77.5, 0f, 0f)
    );

    /**
     * Key into {@link #VIEWPOINTS} used by the bare {@code /bfp new_tutorial} (no section
     * argument), mirroring {@code /bfp tutorial}'s no-arg quick-test form. Repoint this once a
     * dedicated entrance viewpoint is captured; for now the only known-good spot is Officer Cruz's.
     */
    public static final String DEFAULT_VIEWPOINT = "officer_cruz";

    private NewTutBuildingManager() {}

    /** Call once from {@code ServerStartedEvent}. */
    public static void place(ServerLevel level) {
        boolean placed = new SchemLoader(Identifier.fromNamespaceAndPath(BerongSMP.MODID, SCHEM_PATH))
                .place(level, POS);
        if (placed) {
            BerongSMP.LOGGER.info("New tutorial building placed at {}", POS);
            discardDuplicateCruz(level);
        } else {
            BerongSMP.LOGGER.error("Failed to place new tutorial building ({}) at {}", SCHEM_PATH, POS);
        }
    }

    /** See {@link #DUPLICATE_CRUZ_BOUNDS}. Runs after every placement, not just once. */
    private static void discardDuplicateCruz(ServerLevel level) {
        List<CustomNpcEntity> duplicates = level.getEntitiesOfClass(CustomNpcEntity.class, DUPLICATE_CRUZ_BOUNDS,
                npc -> npc.getNpcType() == NpcType.OFFICER_CRUZ);
        for (CustomNpcEntity npc : duplicates) {
            npc.discard();
        }
        if (!duplicates.isEmpty()) {
            BerongSMP.LOGGER.info("Discarded {} duplicate Officer Cruz entity/entities from the old tunnel-finish spot", duplicates.size());
        }
    }
}
