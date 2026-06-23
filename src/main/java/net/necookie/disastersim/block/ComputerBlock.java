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

public class ComputerBlock extends Block {

    public static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT     = BlockStateProperties.LIT;
    public static final BooleanProperty BURNING = BooleanProperty.create("burning");
    public static final BooleanProperty BROKEN  = BooleanProperty.create("broken");

    private static final VoxelShape SHAPE_NORTH = Block.box(1, 0, 3, 15, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1, 0, 0, 15, 14, 13);
    private static final VoxelShape SHAPE_WEST  = Block.box(3, 0, 1, 16, 14, 15);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 0, 1, 13, 14, 15);

    public ComputerBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(BURNING, false)
                .setValue(BROKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, BURNING, BROKEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(LIT, false)
                .setValue(BURNING, false)
                .setValue(BROKEN, false);
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (state.getValue(BROKEN)) {
            player.sendSystemMessage(Component.literal(
                    "§8[DAMAGED] This computer was destroyed by fire. Replace it."));
            return InteractionResult.SUCCESS;
        }

        if (state.getValue(BURNING)) {
            player.sendSystemMessage(Component.literal(
                    "§c⚠ Already on fire! Use a §a[CO2 Extinguisher]§c to suppress it!"));
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(LIT, true).setValue(BURNING, true), 3);

            for (Direction dir : Direction.values()) {
                BlockPos adj = pos.relative(dir);
                if (level.getBlockState(adj).isAir()) {
                    level.setBlock(adj, Blocks.FIRE.defaultBlockState(), 3);
                }
            }

            player.sendSystemMessage(Component.literal(
                    "§c⚠ Electrical fire started! §7Use a §a[CO2 Extinguisher]§7 to put it out!"));
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

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (state.getValue(BROKEN)) {
            player.sendSystemMessage(Component.literal(
                    "§8[DAMAGED] This computer was destroyed by fire. Replace it."));
            return InteractionResult.SUCCESS;
        }

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

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(BURNING);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BURNING)) return;

        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (level.getBlockState(adj).isAir()) {
                level.setBlock(adj, Blocks.FIRE.defaultBlockState(), 3);
            }
        }

        // Seed fire next to flammable blocks; vanilla fire propagates from there.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (checkState.ignitedByLava() && random.nextInt(5) == 0) {
                        BlockPos above = checkPos.above();
                        if (level.getBlockState(above).isAir()) {
                            level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }

                    if (checkState.isAir() && random.nextInt(8) == 0) {
                        for (Direction checkDir : Direction.values()) {
                            if (level.getBlockState(checkPos.relative(checkDir)).ignitedByLava()) {
                                level.setBlock(checkPos, Blocks.FIRE.defaultBlockState(), 3);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (state.getValue(BURNING)) {
            for (int i = 0; i < 5; i++) {
                level.addParticle(ParticleTypes.FLAME,
                        pos.getX() + 0.1 + rand.nextDouble() * 0.8,
                        pos.getY() + 0.82 + rand.nextDouble() * 0.25,
                        pos.getZ() + 0.1 + rand.nextDouble() * 0.8,
                        (rand.nextDouble() - 0.5) * 0.04,
                        0.05 + rand.nextDouble() * 0.06,
                        (rand.nextDouble() - 0.5) * 0.04);
            }

            for (Direction sideDir : new Direction[]{
                    Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                if (rand.nextInt(3) == 0) {
                    double ex = pos.getX() + 0.5 + sideDir.getStepX() * 0.44;
                    double ey = pos.getY() + 0.2 + rand.nextDouble() * 0.6;
                    double ez = pos.getZ() + 0.5 + sideDir.getStepZ() * 0.44;
                    level.addParticle(ParticleTypes.FLAME, ex, ey, ez,
                            sideDir.getStepX() * 0.04, 0.02, sideDir.getStepZ() * 0.04);
                }
            }

            if (rand.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        pos.getX() + 0.2 + rand.nextDouble() * 0.6,
                        pos.getY() + 0.4 + rand.nextDouble() * 0.5,
                        pos.getZ() + 0.2 + rand.nextDouble() * 0.6,
                        (rand.nextDouble() - 0.5) * 0.03, 0.04, (rand.nextDouble() - 0.5) * 0.03);
            }

            if (rand.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.1 + rand.nextDouble() * 0.8,
                        pos.getY() + 0.1 + rand.nextDouble() * 0.9,
                        pos.getZ() + 0.1 + rand.nextDouble() * 0.8,
                        (rand.nextDouble() - 0.5) * 0.20, 0.07 + rand.nextDouble() * 0.05,
                        (rand.nextDouble() - 0.5) * 0.20);
            }

            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.1 + rand.nextDouble() * 0.8,
                        pos.getY() + 0.85 + rand.nextDouble() * 0.2,
                        pos.getZ() + 0.1 + rand.nextDouble() * 0.8,
                        (rand.nextDouble() - 0.5) * 0.01,
                        0.07 + rand.nextDouble() * 0.05,
                        (rand.nextDouble() - 0.5) * 0.01);
            }

            if (rand.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.LAVA,
                        pos.getX() + 0.2 + rand.nextDouble() * 0.6,
                        pos.getY() + 0.4 + rand.nextDouble() * 0.4,
                        pos.getZ() + 0.2 + rand.nextDouble() * 0.6,
                        0, 0, 0);
            }

        } else if (state.getValue(BROKEN)) {
            // Still smoulders after being doused with CO2.
            if (rand.nextInt(15) == 0) {
                level.addParticle(ParticleTypes.SMOKE,
                        pos.getX() + 0.25 + rand.nextDouble() * 0.5,
                        pos.getY() + 0.8,
                        pos.getZ() + 0.25 + rand.nextDouble() * 0.5,
                        0, 0.025, 0);
            }

        } else if (state.getValue(LIT)) {
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
