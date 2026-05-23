package net.necookie.disastersim.world.building.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.api.building.BuildingComponent;

/**
 * A 10x12 laboratory room featuring continuous white desks and black computer monitors.
 */
public class CCS_Lab_Module implements BuildingComponent {
    @Override
    public void place(Level level, BlockPos pos) {
        // Walls and Floor
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 12; z++) {
                // Floor: Updated to light tiles (Polished Diorite) to match the photo
                level.setBlock(pos.offset(x, 0, z), Blocks.POLISHED_DIORITE.defaultBlockState(), 3);

                // Ceiling
                level.setBlock(pos.offset(x, 4, z), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3);

                // Walls: White concrete
                if (x == 0 || x == 9 || z == 0 || z == 11) {
                    for (int y = 1; y < 4; y++) {
                        level.setBlock(pos.offset(x, y, z), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Furniture: Continuous rows of desks with monitors
        for (int z = 2; z < 10; z += 3) {
            for (int x = 1; x < 9; x++) {
                // Leave an aisle in the middle of the room
                if (x == 4 || x == 5) continue;

                BlockPos deskPos = pos.offset(x, 1, z);

                // Continuous white tables (Smooth Quartz)
                level.setBlock(deskPos, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);

                // Monitors: Updated to Black Concrete to match the photo
                level.setBlock(deskPos.above(), Blocks.BLACK_CONCRETE.defaultBlockState(), 3);

                // "Keyboard/Mouse" on the desk
                level.setBlock(deskPos.offset(0, 1, 1), Blocks.STONE_BUTTON.defaultBlockState(), 3);
            }
        }
    }
}