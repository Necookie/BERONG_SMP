package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wide oak desk; FACING drives the model only — collision is a uniform box. Flammable. */
public class ComputerTableBlock extends FlammableFacingBlock {

    // Tabletop Y=12-16 covers full XZ; legs go floor to tabletop — collision covers full footprint
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public ComputerTableBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
