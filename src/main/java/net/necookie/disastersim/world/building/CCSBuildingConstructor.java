package net.necookie.disastersim.world.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.necookie.disastersim.world.building.modules.CCS_Lab_Module;
import net.necookie.disastersim.world.building.modules.LSPU_Facade_Module;
import net.necookie.disastersim.world.building.modules.LSPU_Hallway_Module;

/**
 * Assembler for the LSPU CCS Building Digital Twin.
 */
public class CCSBuildingConstructor {
    public void construct(Level level, BlockPos startPos) {
        // --- 1st Floor ---
        // Basic Lobby/Hallway
        LSPU_Hallway_Module groundFloorHall = new LSPU_Hallway_Module(30);
        groundFloorHall.place(level, startPos);

        // --- 2nd Floor ---
        BlockPos secondFloorPos = startPos.above(5);
        
        // Central Hallway
        LSPU_Hallway_Module secondFloorHall = new LSPU_Hallway_Module(30);
        secondFloorHall.place(level, secondFloorPos);

        // Labs on the left side
        CCS_Lab_Module lab1 = new CCS_Lab_Module();
        lab1.place(level, secondFloorPos.offset(-10, 0, 0));
        
        CCS_Lab_Module lab2 = new CCS_Lab_Module();
        lab2.place(level, secondFloorPos.offset(-10, 0, 15));

        // --- Exterior Facade ---
        LSPU_Facade_Module facade = new LSPU_Facade_Module(30, 10);
        facade.place(level, startPos.offset(0, 0, -1));
        facade.place(level, startPos.offset(5, 0, -1)); // Simple placement for demo
    }
}
