package net.necookie.disastersim.world.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Static factory for placing various furniture items and simulation props.
 * Used by modular building components to populate interiors.
 */
public class FurnitureFactory {

    /**
     * Places a fire cabinet against a wall.
     * Includes a red back wall and a glass front.
     * 
     * @param level The level to place in.
     * @param pos The base position of the cabinet.
     * @param facing The direction the glass front should face.
     */
    public static void placeFireCabinet(Level level, BlockPos pos, Direction facing) {
        // Back wall of the cabinet (Red Concrete for visibility)
        level.setBlock(pos, Blocks.RED_CONCRETE.defaultBlockState(), 3);
        
        // Glass front panel
        level.setBlock(pos.relative(facing), Blocks.GLASS_PANE.defaultBlockState(), 3);
        
        // Note: Future implementation could add an inventory with a fire extinguisher item here.
    }

    /**
     * Places a student desk configuration consisting of a table and a seat.
     * 
     * @param level The level to place in.
     * @param pos The position of the desk (table part).
     */
    public static void placeStudentDesk(Level level, BlockPos pos) {
        // Table top (using upside-down oak stairs for a thin table look)
        level.setBlock(pos, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.FACING, Direction.NORTH), 3);
                
        // Seat (using an oak slab)
        level.setBlock(pos.offset(0, 0, 1), Blocks.OAK_SLAB.defaultBlockState(), 3);
    }
}
