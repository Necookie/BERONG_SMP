package net.necookie.disastersim.world.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.necookie.disastersim.world.building.modules.CcsLabModule;
import net.necookie.disastersim.world.building.modules.LspuFacadeModule;
import net.necookie.disastersim.world.building.modules.LspuHallwayModule;

/**
 * Assembler for the LSPU CCS (College of Computer Studies) building digital twin.
 *
 * <p>Orchestrates multiple {@link net.necookie.disastersim.api.building.BuildingComponent}
 * modules — hallways, labs, and the exterior facade — into a cohesive multi-storey
 * structure. Each module is responsible for its own internal geometry; this class
 * is responsible only for positioning them relative to {@code startPos}.
 *
 * <p>Note: this constructor is not currently wired into the active simulation path.
 * The live simulation uses the pre-built {@code lspulibrarymain} NBT structure instead.
 */
public class CCSBuildingConstructor {

    // Floor heights relative to startPos.Y
    private static final int FLOOR_HEIGHT = 5; // blocks between floor levels (wall + slab)

    // Lab X offset: labs branch off to the left of the hallway
    private static final int LAB_X_OFFSET = -10;

    // Z positions for the two second-floor labs along the hallway
    private static final int LAB_1_Z = 0;
    private static final int LAB_2_Z = 15;

    // Facade is placed one block in front of the hallway (negative Z)
    private static final int FACADE_Z_OFFSET = -1;

    // Second facade section starts 5 blocks along X from the first
    private static final int FACADE_2_X_OFFSET = 5;

    /**
     * Constructs the full CCS building at the specified starting position.
     *
     * @param level    The level in which to construct the building.
     * @param startPos The base bottom-northwest corner of the building.
     */
    public void construct(Level level, BlockPos startPos) {

        // --- Ground floor ---
        new LspuHallwayModule(30).place(level, startPos);

        // --- Second floor (offset one floor height upward) ---
        BlockPos secondFloor = startPos.above(FLOOR_HEIGHT);
        new LspuHallwayModule(30).place(level, secondFloor);

        // Labs on the left side of the second-floor hallway
        new CcsLabModule().place(level, secondFloor.offset(LAB_X_OFFSET, 0, LAB_1_Z));
        new CcsLabModule().place(level, secondFloor.offset(LAB_X_OFFSET, 0, LAB_2_Z));

        // --- Exterior facade (placed along the front of the ground floor) ---
        LspuFacadeModule facade = new LspuFacadeModule(30, 10);
        facade.place(level, startPos.offset(0,               0, FACADE_Z_OFFSET));
        facade.place(level, startPos.offset(FACADE_2_X_OFFSET, 0, FACADE_Z_OFFSET));
    }
}
