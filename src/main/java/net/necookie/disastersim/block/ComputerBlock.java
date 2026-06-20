package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Computer/monitor block with FACING and LIT states.
 * Right-click toggles the screen on/off. When on: glows and emits electric spark particles.
 * LIT=true is the "electrical fire" state used for Class C extinguisher training.
 */
public class ComputerBlock extends Block {

    public static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape SHAPE_NORTH = Block.box(1, 0, 3, 15, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1, 0, 0, 15, 14, 13);
    private static final VoxelShape SHAPE_WEST  = Block.box(3, 0, 1, 16, 14, 15);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 0, 1, 13, 14, 15);

    public ComputerBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(LIT, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        boolean nowLit = !state.getValue(LIT);
        level.setBlock(pos, state.setValue(LIT, nowLit), 3);

        if (nowLit) {
            level.playSound(player, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 1.8f);
            if (level.isClientSide()) {
                for (int i = 0; i < 6; i++) {
                    double ox = (level.getRandom().nextDouble() - 0.5) * 0.6;
                    double oy = level.getRandom().nextDouble() * 0.5 + 0.5;
                    double oz = (level.getRandom().nextDouble() - 0.5) * 0.6;
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                            pos.getX() + 0.5 + ox, pos.getY() + oy, pos.getZ() + 0.5 + oz,
                            0, 0.02, 0);
                }
            }
        } else {
            level.playSound(player, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 0.8f);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(LIT)) return;
        if (rand.nextInt(8) != 0) return;

        double ox = (rand.nextDouble() - 0.5) * 0.4;
        double oy = rand.nextDouble() * 0.4 + 0.7;
        double oz = (rand.nextDouble() - 0.5) * 0.4;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5 + ox, pos.getY() + oy, pos.getZ() + 0.5 + oz,
                0, 0.01, 0);
    }
}
