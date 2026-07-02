package net.necookie.disastersim.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Very small idle motion: every so often the NPC takes one short step near its
 * spawn spot and stops, as if something briefly caught its attention. Not a
 * patrol — it never wanders more than ~2 blocks from where it was placed.
 */
public class MinimalWanderGoal extends Goal {

    private final CustomNpcEntity npc;
    private final double speed;
    private Vec3 anchor;
    private long nextTriggerTick;

    public MinimalWanderGoal(CustomNpcEntity npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (anchor == null) {
            anchor = npc.position();
        }
        if (!npc.getNavigation().isDone()) {
            return false;
        }
        long time = npc.level().getGameTime();
        if (time < nextTriggerTick) {
            return false;
        }
        nextTriggerTick = time + 100 + npc.getRandom().nextInt(300);
        return npc.getRandom().nextInt(3) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return !npc.getNavigation().isDone();
    }

    @Override
    public void start() {
        double angle = npc.getRandom().nextDouble() * Math.PI * 2;
        double dist = 0.8 + npc.getRandom().nextDouble() * 1.2;
        double targetX = anchor.x + Math.cos(angle) * dist;
        double targetZ = anchor.z + Math.sin(angle) * dist;
        npc.getNavigation().moveTo(targetX, anchor.y, targetZ, speed);
    }
}
