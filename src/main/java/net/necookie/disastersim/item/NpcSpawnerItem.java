package net.necookie.disastersim.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.necookie.disastersim.registry.ModEntities;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.necookie.disastersim.entity.NpcType;

/**
 * Right-click on any block face to place the NPC on top of it.
 * The NPC automatically faces toward the player who placed it.
 * Consuming one item per placement (stacksTo(16)).
 */
public class NpcSpawnerItem extends Item {

    private final NpcType npcType;

    public NpcSpawnerItem(NpcType npcType, Properties props) {
        super(props);
        this.npcType = npcType;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;

        ServerLevel level = (ServerLevel) ctx.getLevel();
        BlockPos spawnPos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Player player = ctx.getPlayer();

        CustomNpcEntity npc = new CustomNpcEntity(ModEntities.CUSTOM_NPC.get(), level);
        npc.setNpcType(npcType);
        npc.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

        if (player != null) {
            double dx = player.getX() - (spawnPos.getX() + 0.5);
            double dz = player.getZ() - (spawnPos.getZ() + 0.5);
            npc.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
            npc.yRotO = npc.getYRot();
        }

        level.addFreshEntity(npc);
        ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
