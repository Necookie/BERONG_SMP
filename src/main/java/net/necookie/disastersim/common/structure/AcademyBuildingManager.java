package net.necookie.disastersim.common.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.common.structure.SchemLoader;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.entity.NpcType;

import java.util.List;
import java.util.Map;
import net.necookie.disastersim.academy.room2.ReyesRoomManager;

/**
 * Places {@code academy_building.schem} — a WorldEdit {@code //copy -e} capture of the
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
public final class AcademyBuildingManager {

    /** Exact world position requested for the schematic's minimum (0,0,0) corner. */
    public static final BlockPos POS = new BlockPos(-177, -34, 8);

    private static final String SCHEM_PATH = "structure/academy_building.schem";

    /**
     * The Academy routes all of Room 1's dialogue and escorting through a single Officer Cruz
     * anchored at the briefing spot. The duplicate second Cruz the schematic used to bake in at
     * the old tunnel-finish handoff position (~(-123.5,-33,49.5), from the pre-polish two-NPC
     * design) has been **removed from the .schem file itself** (NBT-edited: 29 → 28 entities, both
     * the resource copy and the WorldEdit copy) — so nothing needs discarding on a normal boot
     * anymore. {@link #discardDuplicateCruz} stays as a pure safety net: it scans a generous area
     * of the whole map after every placement (not just this building — see
     * {@link #BUILDING_SCAN_BOUNDS}) and, if more than one {@code OFFICER_CRUZ} exists anywhere
     * (a leftover from a session that predates this fix, a future schematic re-save reintroducing
     * one, an accidental spawner-item copy), keeps only the one nearest {@link #CRUZ_ANCHOR} and
     * discards every other copy. Silent when there's exactly one.
     */
    private static final Vec3 CRUZ_ANCHOR = new Vec3(-153.5, -33.0, 32.5);
    /**
     * The old tunnel-finish handoff Cruz has kept reappearing at this exact spot (a disk-persisted
     * leftover from a session predating the schem-level fix, per the {@link #BUILDING_SCAN_BOUNDS}
     * comment below) even though the "keep nearest anchor" pass alone should already have caught
     * her. {@link #discardDuplicateCruz} now discards anything found within {@link #STRAY_CRUZ_RADIUS}
     * of this point unconditionally, on every boot, before the nearest-anchor pass ever runs — so
     * she's gone from the very start of the session instead of merely losing a tie-break.
     */
    private static final Vec3 STRAY_CRUZ_POS = new Vec3(-122, -33, 49);
    private static final double STRAY_CRUZ_RADIUS = 3.0;
    private static final AABB STRAY_CRUZ_BOUNDS = new AABB(
            STRAY_CRUZ_POS.x - STRAY_CRUZ_RADIUS, STRAY_CRUZ_POS.y - STRAY_CRUZ_RADIUS, STRAY_CRUZ_POS.z - STRAY_CRUZ_RADIUS,
            STRAY_CRUZ_POS.x + STRAY_CRUZ_RADIUS, STRAY_CRUZ_POS.y + STRAY_CRUZ_RADIUS, STRAY_CRUZ_POS.z + STRAY_CRUZ_RADIUS);
    /**
     * Deliberately much wider than the building itself: a Cruz saved to disk from a session
     * predating either the schem-level fix or the escort stuck-recovery logic could have drifted
     * or been left anywhere reachable on the map, and {@code SchemLoader}'s discard-before-place
     * only clears entities inside the schematic's own placement footprint — a stray outside it
     * would otherwise survive forever across reboots. Covers every built structure in the mod
     * (lobby, library, SSC, CCS, this building) with generous margin; still cheap since
     * {@code getEntitiesOfClass} only visits loaded entity sections, not the raw volume.
     */
    private static final AABB BUILDING_SCAN_BOUNDS = new AABB(-400, -80, -200, 400, 60, 400);

    /** A named F3-captured reference viewpoint inside the building, for {@code /bfp tutorial}. */
    public record Viewpoint(double x, double y, double z, float yaw, float pitch) {}

    /**
     * Reference viewpoints for {@code /bfp tutorial <name>} (see
     * {@code BfpAdminCommands.academyTutorialCommand}) — one admin teleport target per named station,
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
     * Key into {@link #VIEWPOINTS} used by the bare {@code /bfp tutorial} (no section
     * argument), mirroring {@code /bfp old_tutorial}'s no-arg quick-test form. Repoint this once a
     * dedicated entrance viewpoint is captured; for now the only known-good spot is Officer Cruz's.
     */
    public static final String DEFAULT_VIEWPOINT = "officer_cruz";

    private AcademyBuildingManager() {}

    /** Call once from {@code ServerStartedEvent}. */
    public static void place(ServerLevel level) {
        boolean placed = new SchemLoader(Identifier.fromNamespaceAndPath(BerongSMP.MODID, SCHEM_PATH))
                .place(level, POS);
        if (placed) {
            BerongSMP.LOGGER.info("New tutorial building placed at {}", POS);
            discardDuplicateCruz(level);
            placeGreenMarks(level);
            // Self-healing: make sure the Tool Selection Wall's 3 extinguisher frames exist and
            // are filled, even if the schematic's own copies failed to spawn for any reason.
            ReyesRoomManager.restockExtinguisherFrames(level);
        } else {
            BerongSMP.LOGGER.error("Failed to place new tutorial building ({}) at {}", SCHEM_PATH, POS);
        }
    }

    /**
     * The stray Officer Cruz at {@link #STRAY_CRUZ_POS} is a disk-persisted leftover sitting in a
     * chunk that's merely block-loaded (from schematic placement), not entity-ticking — Minecraft
     * only attaches a chunk's persisted entities to the level once it's promoted to the
     * entity-ticking ring (in range of an online player), which the boot-time scan in
     * {@link #discardDuplicateCruz} runs long before any player is near. That's why a one-shot
     * boot scan alone could never actually find and discard her: she simply isn't loaded into the
     * level yet at that point. Calling this repeatedly (from {@code AcademyManager.tick()}, every
     * tick, since it's a tiny single-chunk-sized AABB query) guarantees she's discarded the very
     * tick her chunk activates — which only ever happens because a player walked close enough to
     * see her, so in practice she's removed before becoming visible.
     */
    public static void sweepStrayCruz(ServerLevel level) {
        for (CustomNpcEntity npc : level.getEntitiesOfClass(CustomNpcEntity.class, STRAY_CRUZ_BOUNDS,
                npc -> npc.getNpcType() == NpcType.OFFICER_CRUZ)) {
            npc.discard();
            BerongSMP.LOGGER.info("Discarded a stray Officer Cruz entity at the old tunnel-finish handoff spot");
        }
    }

    /** See {@link #CRUZ_ANCHOR}. Runs after every placement, not just once. */
    private static void discardDuplicateCruz(ServerLevel level) {
        sweepStrayCruz(level);

        List<CustomNpcEntity> all = level.getEntitiesOfClass(CustomNpcEntity.class, BUILDING_SCAN_BOUNDS,
                npc -> npc.getNpcType() == NpcType.OFFICER_CRUZ);

        // Only apply the keep-nearest-anchor tie-break when more than one legitimate Cruz remains —
        // a lone survivor (the common case) must never be touched by this pass.
        if (all.size() <= 1) return;

        CustomNpcEntity keep = all.get(0);
        double bestDistSq = keep.position().distanceToSqr(CRUZ_ANCHOR);
        for (CustomNpcEntity npc : all) {
            double distSq = npc.position().distanceToSqr(CRUZ_ANCHOR);
            if (distSq < bestDistSq) {
                keep = npc;
                bestDistSq = distSq;
            }
        }
        int discarded = 0;
        for (CustomNpcEntity npc : all) {
            if (npc != keep) {
                npc.discard();
                discarded++;
            }
        }
        BerongSMP.LOGGER.info("Discarded {} duplicate Officer Cruz entity/entities; kept the one nearest the briefing anchor", discarded);
    }

    /**
     * Floor blocks (Y=-34, flush with the surrounding floor) swapped to lime concrete under the
     * 4 WASD marks — the standing cells one block above these are
     * {@code CruzRoomManager.GREEN_MARKS}. The schematic itself contains no green blocks in the
     * briefing zone at all (verified by parsing it), so without this the marks are invisible
     * trigger points. Lime concrete over green wool: brightest green in vanilla, flat texture
     * that reads as a painted floor marker, non-flammable (Room 2 spawns real fire in this
     * building), and the closest match to {@code AcademyVisuals.DEFAULT_HIGHLIGHT_COLOR}.
     *
     * <p>Permanent per-placement fixup like {@link #discardDuplicateCruz}: {@link SchemLoader}
     * re-places the original floor on every server start, so this must re-run after every
     * placement — never mistake it for a one-time migration.
     */
    private static final List<BlockPos> GREEN_MARK_FLOOR = List.of(
            new BlockPos(-150, -34, 34),
            new BlockPos(-146, -34, 30),
            new BlockPos(-142, -34, 34),
            new BlockPos(-139, -34, 30)
    );

    private static void placeGreenMarks(ServerLevel level) {
        for (BlockPos pos : GREEN_MARK_FLOOR) {
            level.setBlock(pos, Blocks.LIME_CONCRETE.defaultBlockState(), 3);
        }
        BerongSMP.LOGGER.info("Placed {} lime concrete WASD marks in the Academy briefing zone", GREEN_MARK_FLOOR.size());
    }

}
