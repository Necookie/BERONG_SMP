package net.necookie.disastersim.world.building.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.api.building.BuildingComponent;

/**
 * A corridor/hallway module component.
 * Dimensions: 5 blocks wide, variable length.
 * Features: Tiled floors, overhead lighting, and two-tone wall design.
 */
public class LSPU_Hallway_Module implements BuildingComponent {
    /** Length of the hallway section. */
    private final int length;

    /**
     * Constructor for a hallway module.
     * 
     * @param length The total length of the corridor in blocks.
     */
    public LSPU_Hallway_Module(int length) {
        this.length = length;
    }

    /**
     * Places the hallway section in the world.
     */
    @Override
    public void place(Level level, BlockPos pos) {
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < 5; x++) {
                // Floor: Light-colored tiles (Polished Diorite)
                level.setBlock(pos.offset(x, 0, z), Blocks.POLISHED_DIORITE.defaultBlockState(), 3);

                // Ceiling: White concrete
                level.setBlock(pos.offset(x, 4, z), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);

                // Overhead Lighting: Sea Lanterns centered and placed every 3 blocks
                if (z % 3 == 0 && x == 2) {
                    level.setBlock(pos.offset(x, 4, z), Blocks.SEA_LANTERN.defaultBlockState(), 3);
                }

                // Side Walls (Left and Right sides)
                if (x == 0 || x == 4) {
                    for (int y = 1; y < 4; y++) {
                        // Two-tone wall: Light Blue base (1 block high) with White upper section
                        if (y == 1) {
                            level.setBlock(pos.offset(x, y, z), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(), 3);
                        } else {
                            level.setBlock(pos.offset(x, y, z), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);
                        }
                    }
                }
            }

            // Decorative Emergency Features (Glow Lichen proxying for exit signs) every 5 blocks
            if (z % 5 == 0) {
                level.setBlock(pos.offset(1, 3, z), Blocks.GLOW_LICHEN.defaultBlockState(), 3);
            }
        }
    }
}
