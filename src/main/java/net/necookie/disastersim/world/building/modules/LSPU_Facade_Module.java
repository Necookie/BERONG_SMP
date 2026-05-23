package net.necookie.disastersim.world.building.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.api.building.BuildingComponent;

/**
 * Exterior wall section with pillars and trim.
 */
public class LSPU_Facade_Module implements BuildingComponent {
    private final int width;
    private final int height;

    public LSPU_Facade_Module(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void place(Level level, BlockPos pos) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Wall (Green Concrete)
                level.setBlock(pos.offset(x, y, 0), Blocks.GREEN_CONCRETE.defaultBlockState(), 3);

                // Pillars (Blue Concrete every 5 blocks)
                if (x % 5 == 0) {
                    level.setBlock(pos.offset(x, y, 0), Blocks.BLUE_CONCRETE.defaultBlockState(), 3);
                }

                // Roof Trim (Red Concrete at the top)
                if (y == height - 1) {
                    level.setBlock(pos.offset(x, y, 0), Blocks.RED_CONCRETE.defaultBlockState(), 3);
                }
            }
        }
    }
}
