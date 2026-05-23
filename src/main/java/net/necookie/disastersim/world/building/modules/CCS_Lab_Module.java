package net.necookie.disastersim.world.building.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.api.building.BuildingComponent;

/**
 * A modular laboratory room component.
 * Dimensions: 10x12 blocks.
 * Features: Light-colored tiled floor, continuous white desks, and black computer monitors.
 */
public class CCS_Lab_Module implements BuildingComponent {

    /**
     * Places the laboratory room in the world.
     */
    @Override
    public void place(Level level, BlockPos pos) {
        // --- Structural Logic (Walls, Floor, Ceiling) ---
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 12; z++) {
                // Floor: Light tiles (Polished Diorite)
                level.setBlock(pos.offset(x, 0, z), Blocks.POLISHED_DIORITE.defaultBlockState(), 3);

                // Ceiling: Smooth stone slabs
                level.setBlock(pos.offset(x, 4, z), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3);

                // Outer Walls: 4-block high White Concrete
                if (x == 0 || x == 9 || z == 0 || z == 11) {
                    for (int y = 1; y < 4; y++) {
                        level.setBlock(pos.offset(x, y, z), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);
                    }
                }
            }
        }

        // --- Interior Logic (Furniture and Computers) ---
        // Arrange desks in three main rows (Z-axis)
        for (int rowZ = 2; rowZ < 10; rowZ += 3) {
            for (int colX = 1; colX < 9; colX++) {
                // Leave a 2-block wide center aisle (X = 4, 5)
                if (colX == 4 || colX == 5) continue;

                BlockPos deskPos = pos.offset(colX, 1, rowZ);

                // Continuous white tables (Smooth Quartz blocks)
                level.setBlock(deskPos, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);

                // Computer Monitors (Black Concrete blocks placed on the desk)
                level.setBlock(deskPos.above(), Blocks.BLACK_CONCRETE.defaultBlockState(), 3);

                // Peripheral Simulation (Stone Button as a keyboard/mouse proxy)
                level.setBlock(deskPos.offset(0, 1, 1), Blocks.STONE_BUTTON.defaultBlockState(), 3);
            }
        }
    }
}
