package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.necookie.disastersim.Config;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;

/**
 * Wall-mounted fire alarm pull station.
 *
 * State machine:
 *   ACTIVATED=false → right-click during active FIRE sim → ACTIVATED=true
 *   ACTIVATED=true  → already activated (shows message, no re-trigger)
 *
 * Resets automatically when simulation ends because endSimulation restores
 * the full LSPU library structure, which replaces all blocks including this one.
 */
public class FireAlarmBlock extends Block {

    public static final Property<Direction> FACING    = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty     ACTIVATED = BooleanProperty.create("activated");

    // Wall-panel shape (6×8 px face, 3 px deep) pressed against the back wall for each facing.
    private static final VoxelShape SHAPE_NORTH = Block.box(5, 4, 13, 11, 12, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(5, 4,  0, 11, 12,  3);
    private static final VoxelShape SHAPE_WEST  = Block.box(13, 4, 5, 16, 12, 11);
    private static final VoxelShape SHAPE_EAST  = Block.box( 0, 4, 5,  3, 12, 11);

    public FireAlarmBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, ACTIVATED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(ACTIVATED, false);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (state.getValue(ACTIVATED)) {
            player.sendSystemMessage(Component.literal("§c[ALARM] Already activated!"));
            return InteractionResult.SUCCESS;
        }

        SimulationSession session = SimulationManager.getSession(player.getUUID());
        if (session == null || session.getState() != SimulationManager.SimulationState.FIRE) {
            player.sendSystemMessage(Component.literal("§7[ALARM] No active fire — alarm not triggered."));
            return InteractionResult.SUCCESS;
        }

        level.setBlock(pos, state.setValue(ACTIVATED, true), 3);
        level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("§c🔔 FIRE ALARM ACTIVATED — Evacuate immediately!"));

        double t = (double)(Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
        double hazardDist = SimulationManager.nearestFireDistance(
                (ServerLevel) level, player.blockPosition());
        session.logger.log("fire_alarm_activate", java.util.Map.of(
                "t",               Math.round(t * 100.0) / 100.0,
                "x",               player.getX(),
                "y",               player.getY(),
                "z",               player.getZ(),
                "hazard_distance", Math.round(hazardDist * 100.0) / 100.0
        ));

        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(ACTIVATED)) return;
        if (rand.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.3,
                    pos.getY() + 0.8,
                    pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.3,
                    0, 0.02, 0);
        }
    }
}
