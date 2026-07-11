package net.necookie.disastersim.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Twin-tank gravity iced-tea/juice dispenser; FACING-only, not flammable (glass/steel). */
public class BeverageJuiceDispenserBlock extends HorizontalFacingBlock {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public BeverageJuiceDispenserBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPE;
    }
}
