package net.necookie.disastersim.world.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.necookie.disastersim.world.building.modules.CCS_Lab_Module;
import net.necookie.disastersim.world.building.modules.LSPU_Facade_Module;
import net.necookie.disastersim.world.building.modules.LSPU_Hallway_Module;

/**
 * Assembler class for the LSPU CCS (College of Computer Studies) Building Digital Twin.
 * Orchestrates multiple building modules to construct a cohesive multi-story structure.
 */
public class CCSBuildingConstructor {

    /**
     * Constructs the full CCS building at the specified starting position.
     * 
     * @param level The level in which to construct the building.
     * @param startPos The base bottom-left corner of the building.
     */
    public void construct(Level level, BlockPos startPos) {
        
        // --- 1st Floor (Ground Floor) ---
        // Basic Lobby/Hallway extending 30 blocks
        LSPU_Hallway_Module groundFloorHall = new LSPU_Hallway_Module(30);
        groundFloorHall.place(level, startPos);

        // --- 2nd Floor ---
        // Offset by 5 blocks upwards to account for wall height + ceiling
        BlockPos secondFloorPos = startPos.above(5);
        
        // Central Hallway for the 2nd floor
        LSPU_Hallway_Module secondFloorHall = new LSPU_Hallway_Module(30);
        secondFloorHall.place(level, secondFloorPos);

        // Labs on the left side (offset -10 in X)
        // Lab 1
        CCS_Lab_Module lab1 = new CCS_Lab_Module();
        lab1.place(level, secondFloorPos.offset(-10, 0, 0));
        
        // Lab 2 (further down the hall)
        CCS_Lab_Module lab2 = new CCS_Lab_Module();
        lab2.place(level, secondFloorPos.offset(-10, 0, 15));

        // --- Exterior Facade ---
        // Places green/blue decorative walls along the side of the building
        LSPU_Facade_Module facade = new LSPU_Facade_Module(30, 10);
        
        // Place facade on both sides of the entrance/hallway
        facade.place(level, startPos.offset(0, 0, -1));
        facade.place(level, startPos.offset(5, 0, -1)); 
    }
}
