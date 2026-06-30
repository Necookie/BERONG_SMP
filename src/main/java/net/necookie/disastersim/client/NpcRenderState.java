package net.necookie.disastersim.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Carries the per-frame texture path for a CustomNpcEntity render cycle. */
public class NpcRenderState extends HumanoidRenderState {
    public Identifier texture = null;
}
