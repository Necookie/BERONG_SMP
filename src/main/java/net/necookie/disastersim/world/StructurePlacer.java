package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Common contract for placing a structure into the world at a given origin. */
public interface StructurePlacer {
    boolean place(ServerLevel level, BlockPos origin);
}
