package net.necookie.disastersim.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.entity.CustomNpcEntity;

/**
 * Renders CustomNpcEntity using the humanoid (player-skin) model.
 * Uses state-based rendering: NpcRenderState carries the texture chosen from
 * the entity's NpcType so the renderer stays stateless.
 *
 * HumanoidMobRenderer type order: <Entity, RenderState, Model>
 */
public class CustomNpcRenderer
        extends HumanoidMobRenderer<CustomNpcEntity, NpcRenderState, HumanoidModel<NpcRenderState>> {

    public CustomNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public NpcRenderState createRenderState() {
        return new NpcRenderState();
    }

    @Override
    public void extractRenderState(CustomNpcEntity entity, NpcRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.texture = Identifier.fromNamespaceAndPath(
                BerongSMP.MODID, "textures/entity/npc/" + entity.getNpcType().id + ".png");
    }

    @Override
    public Identifier getTextureLocation(NpcRenderState state) {
        return state.texture != null
                ? state.texture
                : Identifier.fromNamespaceAndPath(BerongSMP.MODID, "textures/entity/npc/sgt_reyes.png");
    }
}
