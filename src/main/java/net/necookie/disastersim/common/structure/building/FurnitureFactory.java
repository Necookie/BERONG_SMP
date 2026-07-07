package net.necookie.disastersim.common.structure.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;

public class FurnitureFactory {

    public static void placeFireCabinet(Level level, BlockPos pos, Direction facing) {
        level.setBlock(pos, Blocks.RED_CONCRETE.defaultBlockState(), 3);
        level.setBlock(pos.relative(facing), Blocks.GLASS_PANE.defaultBlockState(), 3);
    }

    public static void placeStudentDesk(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.FACING, Direction.NORTH), 3);
        level.setBlock(pos.offset(0, 0, 1), Blocks.OAK_SLAB.defaultBlockState(), 3);
    }
}
