package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bottled-water dispenser; right-click for a drink (sound only). */
public class WaterDispenserBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(3, 0, 3, 13, 15, 13);
    private static final VoxelShape SHAPE_EW = box(3, 0, 3, 13, 15, 13);

    public WaterDispenserBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        level.playSound(player, pos, SoundEvents.GENERIC_DRINK.value(), SoundSource.BLOCKS, 0.6f,
                0.95f + level.getRandom().nextFloat() * 0.1f);
        return InteractionResult.SUCCESS;
    }
}
