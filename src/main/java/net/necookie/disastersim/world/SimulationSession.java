package net.necookie.disastersim.world;

import net.minecraft.server.level.ServerPlayer;

public class SimulationSession {
    public SimulationManager.SimulationState state;
    public int timer;
    public ServerPlayer player;

    public SimulationSession(ServerPlayer player, SimulationManager.SimulationState state) {
        this.player = player;
        this.state  = state;
        this.timer  = SimulationManager.SIM_DURATION_TICKS;
    }
}
