package net.necookie.disastersim.common.zones;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.necookie.disastersim.common.telemetry.TelemetryCsvWriter;
import net.necookie.disastersim.common.simulation.SimulationSession;

/**
 * Defines assembly (safe) zones outside the LSPU Library and CCS Admin Building.
 *
 * During any active simulation, a green particle force-field border is drawn
 * around the perimeter of the active zone every 5 ticks. When the player steps
 * inside, the assembly_area_reached telemetry event fires and the simulation ends.
 *
 * IMPORTANT: CCS_ZONE coordinates are PLACEHOLDER — tune with F3 in-game after
 * running ./gradlew runServer. See docs/f3_tuning_todo.md for the full checklist.
 */
public class AssemblyZone {

    // Verified — open area north of the LSPU Library main entrance.
    private static final AABB ZONE = new AABB(30, -35, 64, 76, -28, 82);

    // Outside south face of CCS Admin Building (building ends at Z=72).
    // Z:73–90 = open area directly south of the building — the designed assembly area.
    // PLACEHOLDER — walk with F3 to confirm exact coordinates (see docs/f3_tuning_todo.md).
    private static final AABB CCS_ZONE = new AABB(76, -35, 73, 136, -28, 90);

    // New Sim Building 2.0 assembly zone.
    // PLACEHOLDER — no F3 survey has been done for this building yet (unlike ZONE/CCS_ZONE,
    // which are at least approximately walked). Sized/shaped like CCS_ZONE, roughly outside the
    // Lobby's west-facing entrance (docs/new_sim_building2_rooms.md: Lobby X -118..-81, Z
    // 482..498) — MUST be F3-verified in-game before this scenario is trusted for real data
    // collection; see the New Sim Building 2.0 plan's manual prerequisites.
    private static final AABB NEW_SIM2_ZONE = new AABB(-140, -34, 480, -119, -28, 500);

    private static final double PARTICLE_STEP = 1.0;
    private static final int    WALL_HEIGHT   = 5;

    /**
     * Spawns the green force-field border particles around the active zone perimeter.
     * Call every 5 server ticks from SimulationManager while a simulation is active.
     * @param isCCS true for CCS Admin Building simulations, false for LSPU Library.
     */
    public static void spawnBorderParticles(ServerLevel level, boolean isCCS) {
        spawnBorderParticlesForZone(level, isCCS ? CCS_ZONE : ZONE);
    }

    /** New Sim Building 2.0's own dispatcher — a 3rd building doesn't fit the existing boolean isCCS shape. */
    public static void spawnBorderParticlesNewSim2(ServerLevel level) {
        spawnBorderParticlesForZone(level, NEW_SIM2_ZONE);
    }

    private static void spawnBorderParticlesForZone(ServerLevel level, AABB zone) {
        double minX = zone.minX, maxX = zone.maxX;
        double minZ = zone.minZ, maxZ = zone.maxZ;
        double baseY = zone.minY;

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

    /**
     * Returns true if the player position is inside the assembly zone for the given scenario.
     * @param isCCS true for CCS Admin Building simulations, false for LSPU Library.
     */
    public static boolean isInside(Vec3 pos, boolean isCCS) {
        return (isCCS ? CCS_ZONE : ZONE).contains(pos);
    }

    /** New Sim Building 2.0's own dispatcher — see {@link #spawnBorderParticlesNewSim2}. */
    public static boolean isInsideNewSim2(Vec3 pos) {
        return NEW_SIM2_ZONE.contains(pos);
    }

    /**
     * Called once when a player first enters the zone during a simulation.
     * Logs the assembly_area_reached event, sends a message, and bursts celebration particles.
     */
    public static void onPlayerArrived(ServerPlayer player, SimulationSession session,
                                       ServerLevel level, double hazardDist) {
        player.sendSystemMessage(Component.literal(
                "§a✓ You reached the ASSEMBLY AREA — you are safe!"));

        double tRounded = Math.round(session.elapsedSeconds() * 100.0) / 100.0;
        double hazRounded = Math.round(hazardDist * 100.0) / 100.0;
        session.logger.log("assembly_area_reached", java.util.Map.of(
                "t",               tRounded,
                "x",               player.getX(),
                "y",               player.getY(),
                "z",               player.getZ(),
                "hazard_distance", hazRounded
        ));
        session.bufferCsvRow(TelemetryCsvWriter.writeRow(
                session.getSessionId(), player.getUUID().toString(),
                session.getState().name().toLowerCase(),
                tRounded, "assembly_area_reached",
                player.getX(), player.getY(), player.getZ(),
                hazRounded, null, null));

        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.x, pos.y + 1, pos.z, 40, 1.0, 0.5, 1.0, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                pos.x, pos.y + 1, pos.z, 15, 0.5, 0.5, 0.5, 0.2);
    }

    public static AABB getZone()       { return ZONE; }
    public static AABB getCcsZone()    { return CCS_ZONE; }
    public static AABB getNewSim2Zone(){ return NEW_SIM2_ZONE; }
}
