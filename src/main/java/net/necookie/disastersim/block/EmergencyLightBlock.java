package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wall emergency light: dark in normal operation; {@code SafetyDeviceManager} flips
 * {@code LIT=true} (light level 10 via registration) on every unit inside an arena while any
 * simulation session runs there - corridors light up the moment a drill starts.
 */
public class EmergencyLightBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE_NS = box(3, 10, 13, 13, 15, 16);
    private static final VoxelShape SHAPE_EW = box(13, 10, 3, 16, 15, 13);

    public EmergencyLightBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.LIT);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }
}
