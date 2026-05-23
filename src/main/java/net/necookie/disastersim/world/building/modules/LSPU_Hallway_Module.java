package net.necookie.disastersim.world.building.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.api.building.BuildingComponent;

/**
 * A 5-block wide corridor matching the building's color palette.
 */
public class LSPU_Hallway_Module implements BuildingComponent {
    private final int length;

    public LSPU_Hallway_Module(int length) {
        this.length = length;
    }

    @Override
    public void place(Level level, BlockPos pos) {
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < 5; x++) {
                // Floor: Polished Diorite to match the light-colored tiles in the facility
                level.setBlock(pos.offset(x, 0, z), Blocks.POLISHED_DIORITE.defaultBlockState(), 3);

                // Ceiling
                level.setBlock(pos.offset(x, 4, z), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);

                // Lighting (Sea Lanterns every 3 blocks)
                if (z % 3 == 0 && x == 2) {
                    level.setBlock(pos.offset(x, 4, z), Blocks.SEA_LANTERN.defaultBlockState(), 3);
                }

                // Walls: Two-tone layout (Light Blue base with White upper walls)
                if (x == 0 || x == 4) {
                    for (int y = 1; y < 4; y++) {
                        if (y == 1) {
                            level.setBlock(pos.offset(x, y, z), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(), 3);
                        } else {
                            level.setBlock(pos.offset(x, y, z), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);
                        }
                    }
                }
            }

            // "Emergency Exit Sign"
            if (z % 5 == 0) {
                level.setBlock(pos.offset(1, 3, z), Blocks.GLOW_LICHEN.defaultBlockState(), 3);
            }
        }
    }
}