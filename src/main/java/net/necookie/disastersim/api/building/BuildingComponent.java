package net.necookie.disastersim.api.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface BuildingComponent {
    void place(Level level, BlockPos pos);
}
