package net.necookie.disastersim.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.necookie.disastersim.block.ComputerBlock;
import net.necookie.disastersim.block.hazard.HazardBlock;
import net.necookie.disastersim.block.hazard.WoodshopSawdustLayerBlock;
import net.necookie.disastersim.world.HazardManager;
import net.necookie.disastersim.world.SimulationManager;
import net.necookie.disastersim.world.SimulationSession;

import java.util.function.Consumer;

/**
 * Dev-only tool for testing hazard prop state transitions without typing {@code /setblock}
 * coordinates — right-click a hazard prop directly, similar to WorldEdit's {@code //wand}.
 *
 * <p>Right-click: toggles a HazardBlock/HazardFacingBlock between normal and hazardous, or steps
 * the sawdust layer's accumulation up by one (wrapping 0-5). Shift+right-click: forces the prop's
 * failure consequence immediately, regardless of its current state or failure timer. Works with
 * or without an active simulation session.
 */
public class HazardWandItem extends Item {

    public HazardWandItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.PASS;
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;

        BlockPos pos = ctx.getClickedPos();
        SimulationSession session = SimulationManager.getSession(player.getUUID());

        if (player.isShiftKeyDown()) {
            boolean forced = HazardManager.forceFailure(level, session, pos, player);
            if (!forced) {
                player.sendSystemMessage(Component.literal("§7That's not a hazard prop."));
                return InteractionResult.PASS;
            }
            return InteractionResult.CONSUME;
        }

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WoodshopSawdustLayerBlock) {
            int next = (state.getValue(WoodshopSawdustLayerBlock.ACCUMULATION) + 1) % 6;
            boolean flashed = HazardManager.setSawdustLevel(level, session, pos, next, player);
            if (!flashed) {
                player.sendSystemMessage(Component.literal("§eSawdust accumulation set to " + next + "/5."));
            }
            return InteractionResult.CONSUME;
        }

        if (state.getBlock() instanceof ComputerBlock) {
            boolean nowBurning = !state.getValue(ComputerBlock.BURNING);
            level.setBlock(pos, state
                    .setValue(ComputerBlock.BURNING, nowBurning)
                    .setValue(ComputerBlock.LIT, nowBurning), 3);
            if (!nowBurning && session != null) session.getHazardTimers().remove(pos);
            player.sendSystemMessage(Component.literal(nowBurning
                    ? "§c⚠ Computer set to burning."
                    : "§a✓ Computer fire cleared."));
            return InteractionResult.CONSUME;
        }

        if (!state.hasProperty(HazardBlock.HAZARDOUS)) {
            player.sendSystemMessage(Component.literal("§7That's not a hazard prop."));
            return InteractionResult.PASS;
        }

        if (state.getValue(HazardBlock.HAZARDOUS)) {
            HazardManager.defuse(level, session, pos);
            player.sendSystemMessage(Component.literal("§a✓ Reset to normal."));
        } else {
            HazardManager.activate(level, session, pos);
            player.sendSystemMessage(Component.literal("§c⚠ Set to hazardous."));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Right-click a hazard prop: toggle normal ↔ hazardous"));
        tooltip.accept(Component.literal("§7(Sawdust layer: steps accumulation 0→5)"));
        tooltip.accept(Component.literal("§cShift+Right-click: force its failure consequence now"));
    }
}
