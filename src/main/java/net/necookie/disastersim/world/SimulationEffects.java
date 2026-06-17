package net.necookie.disastersim.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.necookie.disastersim.Config;

/**
 * Applies the per-tick world effects for each disaster type.
 *
 * <p>This class is intentionally decoupled from session management and networking:
 * it receives only the {@link ServerLevel} it should mutate and operates purely
 * on world state. Effect parameters (spawn counts, intervals, area size) are read
 * from {@link Config} at call time so that server-side config reloads take effect
 * without a restart.
 *
 * <p>All methods must be called on the server thread.
 */
public class SimulationEffects {

    /**
     * Places a small cluster of fire blocks at random air positions within the
     * simulation arena.
     *
     * <p>The number of fires placed and the area they can appear in are controlled
     * by {@link Config#FIRE_SPAWN_COUNT} and {@link Config#SIM_AREA_SIZE}/
     * {@link Config#SIM_AREA_HEIGHT}.
     *
     * @param level The server level containing the simulation arena.
     */
    public void simulateFire(ServerLevel level) {
        int count      = Config.FIRE_SPAWN_COUNT.get();
        int areaSize   = Config.SIM_AREA_SIZE.get();
        int areaHeight = Config.SIM_AREA_HEIGHT.get();

        for (int i = 0; i < count; i++) {
            BlockPos firePos = SimulationManager.SIM_POS.offset(
                    level.getRandom().nextInt(areaSize),
                    level.getRandom().nextInt(areaHeight),
                    level.getRandom().nextInt(areaSize));
            if (level.getBlockState(firePos).isAir()) {
                level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    /**
     * Destroys a small number of randomly chosen non-bedrock blocks within the
     * simulation arena to simulate structural collapse.
     *
     * <p>The number of blocks broken and the area they can appear in are controlled
     * by {@link Config#QUAKE_BREAK_COUNT} and {@link Config#SIM_AREA_SIZE}/
     * {@link Config#SIM_AREA_HEIGHT}.
     *
     * @param level The server level containing the simulation arena.
     */
    public void simulateEarthquake(ServerLevel level) {
        int count      = Config.QUAKE_BREAK_COUNT.get();
        int areaSize   = Config.SIM_AREA_SIZE.get();
        int areaHeight = Config.SIM_AREA_HEIGHT.get();

        for (int i = 0; i < count; i++) {
            BlockPos breakPos = SimulationManager.SIM_POS.offset(
                    level.getRandom().nextInt(areaSize),
                    level.getRandom().nextInt(areaHeight),
                    level.getRandom().nextInt(areaSize));
            if (!level.getBlockState(breakPos).isAir()
                    && level.getBlockState(breakPos).getBlock() != Blocks.BEDROCK) {
                level.destroyBlock(breakPos, false);
            }
        }
    }
}
