package net.necookie.disastersim.world.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Static factory for placing furniture and props.
 */
public class FurnitureFactory {
    /**
     * Places a fire cabinet (glass fronted box).
     */
    public static void placeFireCabinet(Level level, BlockPos pos, Direction facing) {
        // Back wall
        level.setBlock(pos, Blocks.RED_CONCRETE.defaultBlockState(), 3);
        // Glass front
        level.setBlock(pos.relative(facing), Blocks.GLASS_PANE.defaultBlockState(), 3);
        
        // Note: Real implementation would add an inventory with a fire extinguisher here
    }

    /**
     * Places a student desk configuration.
     */
    public static void placeStudentDesk(Level level, BlockPos pos) {
        // Table top
        level.setBlock(pos, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.FACING, Direction.NORTH), 3);
        // Seat
        level.setBlock(pos.offset(0, 0, 1), Blocks.OAK_SLAB.defaultBlockState(), 3);
    }
}
