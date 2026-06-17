package net.necookie.disastersim.world;

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
public class SimulationStructureLoader {

    private final Identifier structureId;

    /**
     * Creates a loader bound to a specific structure template.
     *
     * @param structureId The namespaced resource path of the NBT structure file
     *                    (e.g., {@code berongsmp:lspulibrarymain}).
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
    public boolean placeStructure(ServerLevel level, BlockPos pos) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = manager.get(structureId);

        if (templateOpt.isEmpty()) {
            BerongSMP.LOGGER.error("Failed to load structure: {}", structureId);
            return false;
        }

        StructureTemplate template = templateOpt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false);

        // Flags: 2 = UPDATE_CLIENTS | UPDATE_NOTIFY — notifies neighbours and syncs to clients.
        template.placeInWorld(level, pos, pos, settings, level.getRandom(), 2);
        return true;
    }
}
