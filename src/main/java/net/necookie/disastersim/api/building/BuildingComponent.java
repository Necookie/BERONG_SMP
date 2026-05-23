package net.necookie.disastersim.api.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Interface for modular building components.
 * Allows for reusable world construction units.
 */
public interface BuildingComponent {
    /**
     * Places the component in the world at the specified position.
     * 
     * @param level The level to place the blocks in.
     * @param pos   The base position (usually bottom-left-front corner) of the component.
     */
    void place(Level level, BlockPos pos);
}
