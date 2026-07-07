package net.necookie.disastersim.common.structure.building.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.api.building.BuildingComponent;

/**
 * An exterior wall section representing the decorative facade of the LSPU building.
 *
 * <p>The facade is a flat, two-dimensional panel placed along Z = 0 relative to
 * {@code pos}. Its appearance:
 * <ul>
 *   <li>Green Concrete for the main wall surface.</li>
 *   <li>Blue Concrete vertical pillar every 5 blocks along X.</li>
 *   <li>Red Concrete trim along the top row.</li>
 * </ul>
 */
public class LspuFacadeModule implements BuildingComponent {

    private static final int PILLAR_SPACING = 5; // Blue pillar every N blocks along X

    private final int width;
    private final int height;

    /**
     * Creates a facade section with the given dimensions.
     *
     * @param width  Width in blocks (along the X axis).
     * @param height Height in blocks (along the Y axis).
     */
    public LspuFacadeModule(int width, int height) {
        this.width  = width;
        this.height = height;
    }

    /**
     * Places the facade panel in the world with {@code pos} as the
     * bottom-left corner (Z depth = 1 block).
     */
    @Override
    public void place(Level level, BlockPos pos) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Default: green main wall surface
                var block = Blocks.GREEN_CONCRETE.defaultBlockState();

                // Vertical pillar accent overrides the wall colour
                if (x % PILLAR_SPACING == 0) {
                    block = Blocks.BLUE_CONCRETE.defaultBlockState();
                }

                // Top trim overrides everything on the highest row
                if (y == height - 1) {
                    block = Blocks.RED_CONCRETE.defaultBlockState();
                }

                level.setBlock(pos.offset(x, y, 0), block, 3);
            }
        }
    }
}
