package net.necookie.disastersim.common.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.necookie.disastersim.BerongSMP;

import java.util.Optional;

/**
 * Loads and places NBT structure templates into the world.
 *
 * <p>Separates structure I/O from simulation session logic so that each class
 * has one reason to change. This class has no mutable state and is safe to call
 * from the server thread at any point after the server level is available.
 *
 * <p>Thread safety: all methods must be called on the server thread.
 */
public class SimulationStructureLoader implements StructurePlacer {

    private final Identifier structureId;

    /**
     * Creates a loader bound to a specific structure template.
     *
     * @param structureId The namespaced resource path of the NBT structure file
     *                    (e.g., {@code berongsmp:lspu_library_main}).
     */
    public SimulationStructureLoader(Identifier structureId) {
        this.structureId = structureId;
    }

    /**
     * Fetches the structure template from the level's template manager and places
     * it in the world at {@code pos}.
     *
     * <p>Placement flags use {@code 2} ({@code UPDATE_CLIENTS | UPDATE_NOTIFY}),
     * which notifies neighbours and sends block changes to clients — the standard
     * flags for structure placement.
     *
     * @param level The server level to place the structure in.
     * @param pos   The origin (bottom-northwest corner) of the placement.
     * @return {@code true} if the structure was found and placed, {@code false}
     *         if the template could not be resolved (logged as an error).
     */
    @Override
    public boolean place(ServerLevel level, BlockPos pos) {
        // Retrieve the compiled NBT structure template from the level's structure
        // manager, which scans data/<modid>/structure/ in the mod JAR and any
        // datapack folders.  Returns empty if the file is missing or malformed.
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = manager.get(structureId);

        if (templateOpt.isEmpty()) {
            // Log the error but don't crash — the simulation will still run, just
            // without the correct building in place.
            BerongSMP.LOGGER.error("Failed to load structure: {}", structureId);
            return false;
        }

        StructureTemplate template = templateOpt.get();

        // NONE mirror and rotation means the structure is placed exactly as it was
        // saved in the NBT editor — no flipping or turning.
        // setIgnoreEntities(false) keeps any entities (item frames, armour stands)
        // that were saved as part of the NBT template.
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false);

        // placeInWorld arguments:
        //   level    — the world to place into
        //   pos      — where to anchor the structure (bottom-northwest corner)
        //   pos      — the pivot for rotation/mirror (same as pos since we use NONE)
        //   settings — placement configuration (rotation, mirror, entity handling)
        //   random   — used for random blocks like falling sand (not relevant here)
        //   2        — block update flags: 1 = notify neighbours, 2 = send to clients
        //              Combined flag 2 is UPDATE_CLIENTS which also notifies neighbours.
        template.placeInWorld(level, pos, pos, settings, level.getRandom(), 2);
        return true;
    }
}
