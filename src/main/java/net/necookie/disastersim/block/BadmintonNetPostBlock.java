package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.necookie.disastersim.registry.ModBlocks;
import org.jspecify.annotations.Nullable;

/**
 * Badminton net anchor post (the "stitch"). Placing a second post in a straight line — north,
 * south, east, west, up, or down — within {@link #MAX_DISTANCE} blocks automatically fills every
 * block between the two posts with {@link BadmintonNetMeshBlock}, forming one continuous net
 * regardless of the gap (as long as the path in between is clear air). Only the first matching
 * direction (checked in {@link #SCAN_ORDER}) is filled per placement, so one post is ever the
 * anchor of at most one run. Breaking a post ({@link #playerWillDestroy}) tears its run back down,
 * walking outward in every direction until it hits the first non-mesh block.
 */
public class BadmintonNetPostBlock extends Block {

    private static final int MAX_DISTANCE = 24;
    private static final Direction[] SCAN_ORDER = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
    };

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public BadmintonNetPostBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || oldState.is(this)) {
            return;
        }
        for (Direction dir : SCAN_ORDER) {
            BlockPos partner = findPartner(level, pos, dir);
            if (partner != null) {
                fillRun(level, pos, dir, partner);
                return;
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            for (Direction dir : SCAN_ORDER) {
                clearRun(level, pos, dir);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static @Nullable BlockPos findPartner(BlockGetter level, BlockPos from, Direction dir) {
        for (int i = 1; i <= MAX_DISTANCE; i++) {
            BlockPos candidate = from.relative(dir, i);
            BlockState candidateState = level.getBlockState(candidate);
            if (candidateState.getBlock() instanceof BadmintonNetPostBlock) {
                return candidate;
            }
            if (!candidateState.isAir() && !(candidateState.getBlock() instanceof BadmintonNetMeshBlock)) {
                return null;
            }
        }
        return null;
    }

    private static void fillRun(Level level, BlockPos from, Direction dir, BlockPos to) {
        Direction.Axis meshAxis = perpendicularAxis(dir.getAxis());
        BlockState meshState = ModBlocks.BADMINTON_NET_MESH.get().defaultBlockState()
                .setValue(BadmintonNetMeshBlock.AXIS, meshAxis);
        int steps = Math.abs(to.getX() - from.getX()) + Math.abs(to.getY() - from.getY()) + Math.abs(to.getZ() - from.getZ());
        for (int i = 1; i < steps; i++) {
            level.setBlockAndUpdate(from.relative(dir, i), meshState);
        }
    }

    private static void clearRun(Level level, BlockPos from, Direction dir) {
        BlockPos p = from.relative(dir);
        while (level.getBlockState(p).getBlock() instanceof BadmintonNetMeshBlock) {
            level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            p = p.relative(dir);
        }
    }

    private static Direction.Axis perpendicularAxis(Direction.Axis travelAxis) {
        return switch (travelAxis) {
            case X -> Direction.Axis.Z;
            case Z -> Direction.Axis.X;
            case Y -> Direction.Axis.Y;
        };
    }
}
