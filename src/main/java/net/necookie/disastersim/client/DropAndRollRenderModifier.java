package net.necookie.disastersim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.minecraft.world.entity.Avatar;

/**
 * Purely cosmetic "stop, drop, and roll" visual: while {@link BerongSMP#DROPPED_TICKS} is nonzero
 * for a player (synced from {@code DropAndRollManager}), bends their rendered model into a
 * crouched/prone look by overwriting the same {@link AvatarRenderState} fields vanilla itself uses
 * to drive crouch/swim poses — never the real entity {@code Pose}, hitbox, or collision.
 *
 * <p>{@code accept} runs once per visible player per frame ({@link #register} wires it into
 * {@code RegisterRenderStateModifiersEvent}), so it's written to fail closed: a single
 * {@code instanceof} guard and an early return for the (common) "not dropped" case, no casts that
 * can throw. {@code onRenderPre}/{@code onRenderPost} add a supplementary whole-body rock, wired
 * separately onto the runtime event bus since {@link RenderPlayerEvent} only exposes the render
 * state, not the live entity — {@link #ROLL_TICKS} bridges the two stages.
 */
public class DropAndRollRenderModifier extends AvatarRenderStateModifier {

    /** Ticks remaining at which the tilt/rock reaches full strength (ramps down as the window ends). */
    private static final int EASE_OUT_TICKS = 15;

    private static final ContextKey<Integer> ROLL_TICKS =
            new ContextKey<>(Identifier.fromNamespaceAndPath(BerongSMP.MODID, "roll_ticks"));

    @Override
    public <T extends Avatar & ClientAvatarEntity> void accept(T entity, AvatarRenderState state) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        int ticks = player.getData(BerongSMP.DROPPED_TICKS.get());
        if (ticks <= 0) return;

        float progress = easedProgress(ticks);
        state.isCrouching = true;
        state.swimAmount = Math.max(state.swimAmount, progress);
        state.setRenderData(ROLL_TICKS, ticks);
    }

    static float easedProgress(int ticksRemaining) {
        return ticksRemaining >= EASE_OUT_TICKS ? 1.0f : ticksRemaining / (float) EASE_OUT_TICKS;
    }

    public static void onRenderPre(RenderPlayerEvent.Pre<?> event) {
        int ticks = event.getRenderState().getRenderDataOrDefault(ROLL_TICKS, 0);
        if (ticks <= 0) return;
        float rock = (float) Math.sin(ticks * 0.6) * 18.0f * easedProgress(ticks);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(rock));
    }

    public static void onRenderPost(RenderPlayerEvent.Post<?> event) {
        int ticks = event.getRenderState().getRenderDataOrDefault(ROLL_TICKS, 0);
        if (ticks <= 0) return;
        event.getPoseStack().popPose();
    }
}
