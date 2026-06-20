package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * Computer/monitor block with FACING, LIT, and BURNING states.
 * Right-click toggles the screen on/off (blocked while burning).
 * Flint-and-steel ignites a lit-or-off computer, starting an electrical fire.
 * BURNING=true: spreads vanilla fire via randomTick, emits flame+arc+smoke particles.
 * The ONLY way to extinguish a burning computer is the CO2 extinguisher.
 */
public class ComputerBlock extends Block {

    public static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT     = BlockStateProperties.LIT;
    public static final BooleanProperty BURNING = BooleanProperty.create("burning");

    private static final VoxelShape SHAPE_NORTH = Block.box(1, 0, 3, 15, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1, 0, 0, 15, 14, 13);
    private static final VoxelShape SHAPE_WEST  = Block.box(3, 0, 1, 16, 14, 15);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 0, 1, 13, 14, 15);

    public ComputerBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(BURNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, BURNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(LIT, false)
                .setValue(BURNING, false);
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

    // -----------------------------------------------------------------------
    // Ignition via flint & steel
    // -----------------------------------------------------------------------

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (state.getValue(BURNING)) {
            player.sendSystemMessage(Component.literal(
                    "§c⚠ Already on fire! Use a §a[CO2 Extinguisher]§c to suppress it!"));
            return InteractionResult.SUCCESS;
        }

        // Ignite — works whether the computer is on or off
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(LIT, true).setValue(BURNING, true), 3);
            player.sendSystemMessage(Component.literal(
                    "§c⚠ Electrical fire started! Use a §a[CO2 Extinguisher]§c to put it out!"));
            int dmg = stack.getDamageValue() + 1;
            if (dmg >= stack.getMaxDamage()) {
                stack.shrink(1);
            } else {
                stack.setDamageValue(dmg);
            }
        }
        level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    // -----------------------------------------------------------------------
    // Screen toggle — blocked while burning
    // -----------------------------------------------------------------------

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (state.getValue(BURNING)) {
            player.sendSystemMessage(Component.literal(
                    "§c⚠ The computer is on fire! Use a CO2 extinguisher to suppress the electrical fire!"));
            return InteractionResult.SUCCESS;
        }

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

    // -----------------------------------------------------------------------
    // Fire spread — randomTick only fires when BURNING=true
    // -----------------------------------------------------------------------

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(BURNING);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BURNING)) return;

        // Try to ignite each of the 6 adjacent faces
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (level.getBlockState(adj).isAir() && random.nextInt(4) == 0) {
                level.setBlock(adj, Blocks.FIRE.defaultBlockState(), 3);
            }
        }

        // Fire rises — extra upward spread attempt
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir() && random.nextBoolean()) {
            level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
        }
    }

    // -----------------------------------------------------------------------
    // Visual effects
    // -----------------------------------------------------------------------

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (state.getValue(BURNING)) {
            Direction facing = state.getValue(FACING);
            double fx = facing.getStepX();
            double fz = facing.getStepZ();

            // Screen face center — flames billow outward from the monitor
            double sx = pos.getX() + 0.5 + fx * 0.3;
            double sy = pos.getY() + 0.9;
            double sz = pos.getZ() + 0.5 + fz * 0.3;

            // Orange fire from the screen
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.FLAME,
                        sx + (rand.nextDouble() - 0.5) * 0.55,
                        sy + (rand.nextDouble() - 0.5) * 0.45,
                        sz + (rand.nextDouble() - 0.5) * 0.55,
                        fx * 0.02, 0.05, fz * 0.02);
            }

            // Cyan/blue soul-fire flame for the distinctive electrical arc look
            if (rand.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        sx + (rand.nextDouble() - 0.5) * 0.4,
                        sy + (rand.nextDouble() - 0.5) * 0.35,
                        sz + (rand.nextDouble() - 0.5) * 0.4,
                        fx * 0.01, 0.04, fz * 0.01);
            }

            // Electric arcs erupting across the whole block
            if (rand.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.75,
                        pos.getY() + 0.5 + (rand.nextDouble() - 0.5) * 0.65,
                        pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.75,
                        (rand.nextDouble() - 0.5) * 0.14, 0.07, (rand.nextDouble() - 0.5) * 0.14);
            }

            // Smoke rising from the top
            if (rand.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.3 + rand.nextDouble() * 0.4,
                        pos.getY() + 1.1,
                        pos.getZ() + 0.3 + rand.nextDouble() * 0.4,
                        0, 0.07, 0);
            }
        } else if (state.getValue(LIT)) {
            // Ambient sparks when on but not burning
            if (rand.nextInt(8) != 0) return;
            double ox = (rand.nextDouble() - 0.5) * 0.4;
            double oy = rand.nextDouble() * 0.4 + 0.7;
            double oz = (rand.nextDouble() - 0.5) * 0.4;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5 + ox, pos.getY() + oy, pos.getZ() + 0.5 + oz,
                    0, 0.01, 0);
        }
    }
}
