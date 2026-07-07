package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ceiling smoke detector. Passive on its own - {@code SafetyDeviceManager} scans for nearby fire
 * during active FIRE sessions and flips {@code ALARMING}, which drives the red-LED texture, a
 * blinking red particle, and the beep sound (played by the manager, server-side).
 */
public class SmokeDetectorBlock extends Block {

    public static final BooleanProperty ALARMING = BooleanProperty.create("alarming");

    private static final VoxelShape SHAPE = box(5, 13, 5, 11, 16, 11);

    public SmokeDetectorBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ALARMING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ALARMING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (state.getValue(ALARMING) && rand.nextInt(2) == 0) {
            level.addParticle(new DustParticleOptions(0xFF2020, 0.6f),
                    pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, 0, -0.02, 0);
        }
    }
}
