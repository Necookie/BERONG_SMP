package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.necookie.disastersim.registry.ModSounds;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;
import net.necookie.disastersim.common.simulation.SimulationManager;
import net.necookie.disastersim.common.simulation.SimulationSession;
import net.necookie.disastersim.academy.room2.ReyesRoomManager;

/**
 * Wall-mounted fire alarm pull station.
 *
 * State machine:
 *   ACTIVATED=false → right-click during active FIRE sim → ACTIVATED=true
 *   ACTIVATED=true  → already activated (shows message, no re-trigger)
 *
 * The alarm rings continuously via scheduled block ticks while ACTIVATED=true.
 * The chain self-terminates when the structure is restored (block replaced →
 * pending ScheduledTick for FireAlarmBlock is discarded by the level).
 */
public class FireAlarmBlock extends Block {

    public static final Property<Direction> FACING    = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty     ACTIVATED = BooleanProperty.create("activated");

    /** Interval between alarm beeps (25 ticks = 1.25 s). */
    private static final int BEEP_INTERVAL = 25;

    // Housing body 8×11×3 px + bell dome on top + handle grip protrudes to Z=9
    private static final VoxelShape SHAPE_NORTH = Block.box(4, 3,  9, 12, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(4, 3,  0, 12, 16,  7);
    private static final VoxelShape SHAPE_WEST  = Block.box(9, 3,  4, 16, 16, 12);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 3,  4,  7, 16, 12);

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
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
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

    // ── Interaction ──────────────────────────────────────────────────────────

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player instanceof ServerPlayer sp
                && ReyesRoomManager.tryHandleAlarmPress(sp, pos)) {
            return InteractionResult.SUCCESS;
        }

        if (state.getValue(ACTIVATED)) {
            player.sendSystemMessage(Component.literal("§c[ALARM] Already activated!"));
            return InteractionResult.SUCCESS;
        }

        SimulationSession session = SimulationManager.getSession(player.getUUID());
        if (session == null || !session.getState().isFire()) {
            player.sendSystemMessage(Component.literal("§7[ALARM] No active fire — alarm not triggered."));
            return InteractionResult.SUCCESS;
        }

        level.setBlock(pos, state.setValue(ACTIVATED, true), 3);
        player.sendSystemMessage(Component.literal("§c🔔 FIRE ALARM ACTIVATED — Evacuate immediately!"));

        // Play the alarm immediately, then start the repeating tick chain.
        level.playSound(null, pos, ModSounds.FIRE_ALARM_RING.get(), SoundSource.BLOCKS, 2.0f, 1.8f);
        ((ServerLevel) level).scheduleTick(pos, this, BEEP_INTERVAL);

        session.markAlarmRung();

        double t = session.elapsedSeconds();
        double hazardDist = SimulationManager.nearestFireDistance(
                (ServerLevel) level, player.blockPosition());
        session.logger.log("fire_alarm_activate", java.util.Map.of(
                "t",               Math.round(t * 100.0) / 100.0,
                "x",               player.getX(),
                "y",               player.getY(),
                "z",               player.getZ(),
                "hazard_distance", Math.round(hazardDist * 100.0) / 100.0
        ));
        session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                session.getSessionId(), player.getUUID().toString(),
                session.getState().name().toLowerCase(),
                Math.round(t * 100.0) / 100.0, "fire_alarm_activate",
                player.getX(), player.getY(), player.getZ(),
                Math.round(hazardDist * 100.0) / 100.0,
                "fire_alarm", null));

        return InteractionResult.SUCCESS;
    }

    // ── Scheduled tick — keeps the alarm ringing ──────────────────────────────

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVATED)) return;
        // Vary pitch slightly each ring for a more realistic clapper-bell feel.
        float pitch = 1.7f + random.nextFloat() * 0.2f;
        level.playSound(null, pos, ModSounds.FIRE_ALARM_RING.get(), SoundSource.BLOCKS, 2.0f, pitch);
        level.scheduleTick(pos, this, BEEP_INTERVAL);
    }

    // ── Client-side effects (strobe particles) ────────────────────────────────

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(ACTIVATED)) return;

        Direction facing = state.getValue(FACING);
        // Position particles at the bell dome (top of housing, above block centre).
        double bx = pos.getX() + 0.5 + facing.getStepX() * 0.15;
        double bz = pos.getZ() + 0.5 + facing.getStepZ() * 0.15;
        double byTop = pos.getY() + 1.05; // bell dome height

        // Orange electric-spark strobe from the bell dome
        if (rand.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    bx + (rand.nextDouble() - 0.5) * 0.25,
                    byTop,
                    bz + (rand.nextDouble() - 0.5) * 0.25,
                    0, 0.06, 0);
        }

        // Occasional larger lava-ember for a vivid strobe flash
        if (rand.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.LAVA,
                    bx + (rand.nextDouble() - 0.5) * 0.1,
                    byTop - 0.05,
                    bz + (rand.nextDouble() - 0.5) * 0.1,
                    0, 0.04, 0);
        }

        // Smoke rising from bell to give the alarm a smoky "it's bad out here" feel
        if (rand.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SMOKE,
                    bx, byTop + 0.1, bz,
                    0, 0.02, 0);
        }
    }
}
