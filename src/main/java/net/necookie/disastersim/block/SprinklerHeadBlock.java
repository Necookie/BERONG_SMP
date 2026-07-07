package net.necookie.disastersim.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ceiling sprinkler head. Passive on its own - during an active FIRE session,
 * {@code SafetyDeviceManager} makes any sprinkler near fire rain a water-particle curtain and
 * slowly extinguish fire blocks below it (1 per cycle: it buys evacuation time, it does not win
 * the fire alone).
 */
public class SprinklerHeadBlock extends Block {

    private static final VoxelShape SHAPE = box(6, 11, 6, 10, 16, 10);

    public SprinklerHeadBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
