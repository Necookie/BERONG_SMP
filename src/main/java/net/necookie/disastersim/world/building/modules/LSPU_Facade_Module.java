package net.necookie.disastersim.world.building.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.api.building.BuildingComponent;

/**
 * Exterior wall section representing the facade of a building.
 * Features customizable width/height, pillars, and decorative trim.
 */
public class LSPU_Facade_Module implements BuildingComponent {
    /** Total width of this facade section. */
    private final int width;
    
    /** Total height of this facade section. */
    private final int height;

    /**
     * Constructor for a facade module.
     * 
     * @param width Width in blocks.
     * @param height Height in blocks.
     */
    public LSPU_Facade_Module(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Places the facade section in the world.
     */
    @Override
    public void place(Level level, BlockPos pos) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Main Wall Surface (Green Concrete)
                level.setBlock(pos.offset(x, y, 0), Blocks.GREEN_CONCRETE.defaultBlockState(), 3);

                // Vertical Pillars (Blue Concrete every 5 blocks for structural visual accent)
                if (x % 5 == 0) {
                    level.setBlock(pos.offset(x, y, 0), Blocks.BLUE_CONCRETE.defaultBlockState(), 3);
                }

                // Top Roof Trim (Red Concrete at the highest row)
                if (y == height - 1) {
                    level.setBlock(pos.offset(x, y, 0), Blocks.RED_CONCRETE.defaultBlockState(), 3);
                }
            }
        }
    }
}
