package net.necookie.disastersim.common.structure.building.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.common.structure.building.BuildingComponent;

/**
 * A modular laboratory room component representing a CCS computer lab.
 *
 * <p>Dimensions: 10 blocks wide (X) × 12 blocks deep (Z) × 5 blocks tall (Y).
 * Features a polished-diorite tile floor, white-concrete walls, a smooth-stone-slab
 * ceiling, rows of desk-and-monitor workstations, and a two-block-wide centre aisle.
 */
public class CcsLabModule implements BuildingComponent {

    // Room dimensions
    private static final int WIDTH  = 10; // X
    private static final int DEPTH  = 12; // Z
    private static final int WALL_HEIGHT = 4; // Y, exclusive (blocks at y=1..3)

    // Aisle column indices to skip when placing desks (0-indexed)
    private static final int AISLE_COL_A = 4;
    private static final int AISLE_COL_B = 5;

    /**
     * Places the laboratory room in the world with {@code pos} as the
     * bottom-northwest corner.
     */
    @Override
    public void place(Level level, BlockPos pos) {
        placeStructure(level, pos);
        placeFurniture(level, pos);
    }

    /** Places walls, floor, and ceiling. */
    private void placeStructure(Level level, BlockPos pos) {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                // Floor: light tiles (Polished Diorite)
                level.setBlock(pos.offset(x, 0, z), Blocks.POLISHED_DIORITE.defaultBlockState(), 3);

                // Ceiling: Smooth Stone Slabs
                level.setBlock(pos.offset(x, WALL_HEIGHT, z), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3);

                // Outer walls: 3 blocks high (y = 1..3), White Concrete
                if (x == 0 || x == WIDTH - 1 || z == 0 || z == DEPTH - 1) {
                    for (int y = 1; y < WALL_HEIGHT; y++) {
                        level.setBlock(pos.offset(x, y, z), Blocks.CONCRETE.pick(DyeColor.WHITE).defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /** Places desk-and-monitor rows with a central aisle. */
    private void placeFurniture(Level level, BlockPos pos) {
        // Three desk rows, spaced 3 blocks apart along Z
        for (int rowZ = 2; rowZ < DEPTH - 2; rowZ += 3) {
            for (int colX = 1; colX < WIDTH - 1; colX++) {
                if (colX == AISLE_COL_A || colX == AISLE_COL_B) continue;

                BlockPos deskPos = pos.offset(colX, 1, rowZ);

                // Desk surface (Smooth Quartz)
                level.setBlock(deskPos, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);

                // Monitor (Black Concrete on top of desk)
                level.setBlock(deskPos.above(), Blocks.CONCRETE.pick(DyeColor.BLACK).defaultBlockState(), 3);

                // Keyboard/mouse proxy (Stone Button on monitor level, one block forward)
                level.setBlock(deskPos.offset(0, 1, 1), Blocks.STONE_BUTTON.defaultBlockState(), 3);
            }
        }
    }
}
