package net.necookie.disastersim.common.structure.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.necookie.disastersim.common.structure.building.modules.CcsLabModule;
import net.necookie.disastersim.common.structure.building.modules.LspuFacadeModule;
import net.necookie.disastersim.common.structure.building.modules.LspuHallwayModule;

// Not wired into the active simulation path — the live sim uses lspu_library_main.nbt instead.
public class CCSBuildingConstructor {

    private static final int FLOOR_HEIGHT = 5;
    private static final int LAB_X_OFFSET = -10;
    private static final int LAB_1_Z = 0;
    private static final int LAB_2_Z = 15;
    private static final int FACADE_Z_OFFSET = -1;
    private static final int FACADE_2_X_OFFSET = 5;

    public void construct(Level level, BlockPos startPos) {

        new LspuHallwayModule(30).place(level, startPos);

        BlockPos secondFloor = startPos.above(FLOOR_HEIGHT);
        new LspuHallwayModule(30).place(level, secondFloor);
        new CcsLabModule().place(level, secondFloor.offset(LAB_X_OFFSET, 0, LAB_1_Z));
        new CcsLabModule().place(level, secondFloor.offset(LAB_X_OFFSET, 0, LAB_2_Z));

        LspuFacadeModule facade = new LspuFacadeModule(30, 10);
        facade.place(level, startPos.offset(0,               0, FACADE_Z_OFFSET));
        facade.place(level, startPos.offset(FACADE_2_X_OFFSET, 0, FACADE_Z_OFFSET));
    }
}
