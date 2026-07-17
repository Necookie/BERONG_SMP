package net.necookie.disastersim.client;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Appends a one-line flavor/description to any item or block whose lang file defines a
 * {@code <translation key>.desc} entry (e.g. {@code block.berongsmp.overloaded_microwave.desc}).
 * Items with no {@code .desc} key are left untouched — this never forces placeholder text.
 */
public final class ItemDescriptionTooltip {

    private ItemDescriptionTooltip() {
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String descKey = stack.getItem().getDescriptionId() + ".desc";
        if (Language.getInstance().has(descKey)) {
            event.getToolTip().add(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));
        }
    }
}
