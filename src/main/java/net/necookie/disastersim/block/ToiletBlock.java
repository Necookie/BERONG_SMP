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

/** Ceramic toilet; FACING-only. Right-click plays a flush sound. */
public class ToiletBlock extends HorizontalFacingBlock {

    // Tank at south (Z=11-16), bowl center+north - bounding box covers whole toilet
    private static final VoxelShape SHAPE_NORTH = Block.box(3, 0, 2,  13, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(3, 0, 0,  13, 16, 14);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 0, 3,  14, 16, 13);
    private static final VoxelShape SHAPE_WEST  = Block.box(2, 0, 3,  16, 16, 13);

    public ToiletBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return byFacing(facing, SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        // Satisfying flush sound
        level.playSound(player, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f,
                1.6f + level.getRandom().nextFloat() * 0.2f);
        return InteractionResult.SUCCESS;
    }
}
