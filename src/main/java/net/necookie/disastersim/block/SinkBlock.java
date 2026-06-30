package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall-mounted sink; FACING-only. Right-click plays a water-ambient sound. */
public class SinkBlock extends HorizontalFacingBlock {

    // Wall-mounted sink: basin body + back plate.  FACING=NORTH → back plate on south wall
    private static final VoxelShape SHAPE_NORTH = Block.box(2, 6, 7,  14, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 6, 0,  14, 14, 9);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 6, 2,  9,  14, 14);
    private static final VoxelShape SHAPE_WEST  = Block.box(7, 6, 2,  16, 14, 14);

    public SinkBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        level.playSound(player, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.5f,
                0.9f + level.getRandom().nextFloat() * 0.2f);
        return InteractionResult.SUCCESS;
    }
}
