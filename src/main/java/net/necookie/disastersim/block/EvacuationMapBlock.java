package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.necookie.disastersim.common.safety.SafetyDeviceManager;

/**
 * Wall-mounted evacuation map: right-click to get the nearest emergency exit's name and distance
 * in chat, plus 30 seconds of the Academy compass needle pointing at it (via
 * {@link SafetyDeviceManager#pointToNearestExit}).
 */
public class EvacuationMapBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(1, 2, 14, 15, 14, 16);
    private static final VoxelShape SHAPE_EW = box(14, 2, 1, 16, 14, 15);

    public EvacuationMapBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            SafetyDeviceManager.pointToNearestExit(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
