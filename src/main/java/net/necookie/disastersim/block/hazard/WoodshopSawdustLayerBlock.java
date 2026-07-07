package net.necookie.disastersim.block.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * Woodshop floor substrate — accumulates sawdust from 0 (clean) to 5 (critically thick).
 * Emits ash/smoke particles at accumulation >= 3. Special block: integer state, no facing.
 *
 * <p>Per Items.md #3: state progression (0→5) and the "flash-ignite a 3x3 area" failure
 * consequence at accumulation=5 are driven by {@link net.necookie.disastersim.common.hazard.HazardManager}
 * — this block has no {@code HAZARDOUS} property to hook, so there is no per-block override here.
 */
public class WoodshopSawdustLayerBlock extends Block {

    public static final IntegerProperty ACCUMULATION = IntegerProperty.create("accumulation", 0, 5);

    private static final VoxelShape[] SHAPES = {
        Block.box(0, 0, 0, 16, 1, 16),
        Block.box(0, 0, 0, 16, 2, 16),
        Block.box(0, 0, 0, 16, 3, 16),
        Block.box(0, 0, 0, 16, 4, 16),
        Block.box(0, 0, 0, 16, 5, 16),
        Block.box(0, 0, 0, 16, 6, 16)
    };

    public WoodshopSawdustLayerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ACCUMULATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACCUMULATION);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(ACCUMULATION)];
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (state.getValue(ACCUMULATION) < 3) return;
        if (rand.nextInt(5) != 0) return;
        double x = pos.getX() + rand.nextDouble();
        double y = pos.getY() + 0.05 + rand.nextDouble() * 0.1;
        double z = pos.getZ() + rand.nextDouble();
        level.addParticle(ParticleTypes.ASH, x, y, z, 0.0, 0.02, 0.0);
    }
}
