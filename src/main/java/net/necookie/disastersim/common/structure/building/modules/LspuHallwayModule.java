package net.necookie.disastersim.common.structure.building.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.common.structure.building.BuildingComponent;

/**
 * A corridor/hallway module representing a standard LSPU building passage.
 *
 * <p>Dimensions: 5 blocks wide (X) × {@code length} blocks long (Z) × 5 blocks tall (Y).
 * Design features:
 * <ul>
 *   <li>Polished Diorite tile floor.</li>
 *   <li>White Concrete ceiling with Sea Lantern lighting centred every 3 blocks.</li>
 *   <li>Two-tone walls: Light Blue Concrete at the base row (y = 1), White Concrete above.</li>
 *   <li>Glow Lichen at y = 3 on one wall every 5 blocks, proxying exit signs.</li>
 * </ul>
 */
public class LspuHallwayModule implements BuildingComponent {

    private static final int HALLWAY_WIDTH  = 5;
    private static final int WALL_HEIGHT    = 4; // Y, exclusive (y = 1..3 are walls)
    private static final int CENTRE_COL     = 2; // X index of the hallway centre (for lighting)
    private static final int LIGHT_SPACING  = 3; // Sea Lanterns every N blocks along Z
    private static final int SIGN_SPACING   = 5; // Glow Lichen every N blocks along Z

    private final int length;

    /**
     * Creates a hallway module of the given length.
     *
     * @param length The total length of the corridor in blocks (along the Z axis).
     */
    public LspuHallwayModule(int length) {
        this.length = length;
    }

    /**
     * Places the hallway section in the world with {@code pos} as the
     * bottom-northwest corner.
     */
    @Override
    public void place(Level level, BlockPos pos) {
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < HALLWAY_WIDTH; x++) {
                // Floor: light tiles (Polished Diorite)
                level.setBlock(pos.offset(x, 0, z), Blocks.POLISHED_DIORITE.defaultBlockState(), 3);

                // Ceiling: White Concrete (overwritten by sea lantern where applicable)
                level.setBlock(pos.offset(x, WALL_HEIGHT, z), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);

                // Overhead lighting: Sea Lantern centred on the ceiling every LIGHT_SPACING blocks
                if (z % LIGHT_SPACING == 0 && x == CENTRE_COL) {
                    level.setBlock(pos.offset(x, WALL_HEIGHT, z), Blocks.SEA_LANTERN.defaultBlockState(), 3);
                }

                // Side walls: left (x=0) and right (x=4)
                if (x == 0 || x == HALLWAY_WIDTH - 1) {
                    for (int y = 1; y < WALL_HEIGHT; y++) {
                        // Two-tone: Light Blue base (y=1), White Concrete above
                        var block = (y == 1)
                                ? Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
                                : Blocks.WHITE_CONCRETE.defaultBlockState();
                        level.setBlock(pos.offset(x, y, z), block, 3);
                    }
                }
            }

            // Emergency exit sign proxy (Glow Lichen) on the left wall at head height
            if (z % SIGN_SPACING == 0) {
                level.setBlock(pos.offset(1, 3, z), Blocks.GLOW_LICHEN.defaultBlockState(), 3);
            }
        }
    }
}
