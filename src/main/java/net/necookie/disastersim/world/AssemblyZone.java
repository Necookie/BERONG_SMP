package net.necookie.disastersim.world;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.Config;

/**
 * Defines the assembly (safe) zone outside the LSPU Library.
 *
 * During any active simulation, a green particle force-field border is drawn
 * around the perimeter of the zone every 5 ticks. When the player steps inside,
 * the assembly_area_reached telemetry event fires and the simulation ends.
 *
 * IMPORTANT: ZONE coordinates are PLACEHOLDER — tune with F3 in-game after
 * running ./gradlew runServer. The zone should be a clear open area outside
 * the library's main exit.
 */
public class AssemblyZone {

    // PLACEHOLDER — open area in front of the library main entrance (Z- face, before Z≈83).
    // Students approach from the lobby (Z≈0) so the front face is the low-Z side.
    // Adjust min/max after walking in-game with F3.
    private static final AABB ZONE = new AABB(30, -35, 64, 76, -28, 82);

    private static final double PARTICLE_STEP = 1.0;
    private static final int    WALL_HEIGHT   = 5;

    /**
     * Spawns the green force-field border particles around the zone perimeter.
     * Call every 5 server ticks from SimulationManager while a simulation is active.
     */
    public static void spawnBorderParticles(ServerLevel level) {
        double minX = ZONE.minX, maxX = ZONE.maxX;
        double minZ = ZONE.minZ, maxZ = ZONE.maxZ;
        double baseY = ZONE.minY;

        for (int layer = 0; layer < WALL_HEIGHT; layer++) {
            double y = baseY + layer;
            for (double x = minX; x <= maxX; x += PARTICLE_STEP) {
                spawnGreen(level, x, y, minZ);
                spawnGreen(level, x, y, maxZ);
            }
            for (double z = minZ + PARTICLE_STEP; z < maxZ; z += PARTICLE_STEP) {
                spawnGreen(level, minX, y, z);
                spawnGreen(level, maxX, y, z);
            }
        }
    }

    private static void spawnGreen(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.1, 0.1, 0.1, 0.0);
        if (level.getRandom().nextInt(4) == 0) {
            level.sendParticles(ParticleTypes.SCRAPE, x, y + 0.5, z, 1, 0.05, 0.2, 0.05, 0.0);
        }
    }

    /** Returns true if the player position is inside the assembly zone. */
    public static boolean isInside(Vec3 pos) {
        return ZONE.contains(pos);
    }

    /**
     * Called once when a player first enters the zone during a simulation.
     * Logs the assembly_area_reached event, sends a message, and bursts celebration particles.
     */
    public static void onPlayerArrived(ServerPlayer player, SimulationSession session,
                                       ServerLevel level, double hazardDist) {
        player.sendSystemMessage(Component.literal(
                "§a✓ You reached the ASSEMBLY AREA — you are safe!"));

        double t = (double)(Config.SIM_DURATION_TICKS.get() - session.getTimerTicks()) / 20.0;
        double tRounded = Math.round(t * 100.0) / 100.0;
        double hazRounded = Math.round(hazardDist * 100.0) / 100.0;
        session.logger.log("assembly_area_reached", java.util.Map.of(
                "t",               tRounded,
                "x",               player.getX(),
                "y",               player.getY(),
                "z",               player.getZ(),
                "hazard_distance", hazRounded
        ));
        TelemetryCsvWriter.writeRow(
                session.getSessionId(), player.getUUID().toString(),
                session.getState().name().toLowerCase(),
                tRounded, "assembly_area_reached",
                player.getX(), player.getY(), player.getZ(),
                hazRounded, null, null);

        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.x, pos.y + 1, pos.z, 40, 1.0, 0.5, 1.0, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                pos.x, pos.y + 1, pos.z, 15, 0.5, 0.5, 0.5, 0.2);
    }

    public static AABB getZone() { return ZONE; }
}
