package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.neoforge.NeoForgeAdapter;
import com.sk89q.worldedit.regions.Region;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Locale;

/**
 * {@code //copyroom} — reads the caller's active WorldEdit cuboid selection, computes room
 * dimensions/areas/volume, and copies a CSV summary to the server machine's OS clipboard.
 * <p>
 * A dev tool in the same spirit as {@code /bfp} and {@link net.necookie.disastersim.item.HazardWandItem}:
 * it assumes an admin running server + client on the same machine during dev/test, since a real
 * dedicated server has no meaningful way to reach a remote player's client clipboard.
 * <p>
 * Registered as a raw Brigadier literal named {@code "/copyroom"} (leading slash included in the
 * literal string) rather than through WorldEdit's own Piston command manager — this is the exact
 * mechanism WorldEdit itself uses for its {@code //pos1}-style commands (see
 * {@code com.sk89q.worldedit.command.SelectionCommands}, whose {@code @Command} names literally
 * start with {@code "/"}), so typing {@code //copyroom} in chat resolves the same way.
 */
public final class CopyRoomCommand {

    private CopyRoomCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("/copyroom")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(CopyRoomCommand::run));
    }

    private static int run(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        // Checked first, before any WorldEdit symbol is touched: WorldEdit is a compileOnly/
        // localRuntime dependency (see build.gradle), not a hard requirement of this mod, so a
        // server running without it installed must fail cleanly here rather than crash with a
        // NoClassDefFoundError from the WorldEdit calls below.
        if (!ModList.get().isLoaded("worldedit")) {
            source.sendFailure(Component.literal("§cWorldEdit is not installed on this server."));
            return 0;
        }

        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("§cThis command can only be run by a player."));
            return 0;
        }
        ServerPlayer nativePlayer = source.getPlayer();

        Region region;
        try {
            Player wePlayer = NeoForgeAdapter.get().fromNativePlayer(nativePlayer);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
            region = session.getSelection();
        } catch (IncompleteRegionException e) {
            source.sendFailure(Component.literal(
                    "§cNo valid WorldEdit selection. Set both //pos1 and //pos2 first."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                    "§cCould not read WorldEdit selection: " + e.getMessage()));
            return 0;
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        int width = max.x() - min.x() + 1;
        int length = max.z() - min.z() + 1;
        int height = max.y() - min.y() + 1;
        long floorArea = (long) width * length;
        long wallArea = 2L * (width * height) + 2L * (length * height);
        long ceilingArea = floorArea;
        long volume = floorArea * height;

        String csv = "Pos1X,Pos1Y,Pos1Z,Pos2X,Pos2Y,Pos2Z,Width,Length,Height,FloorArea,WallArea,CeilingArea,Volume\n"
                + String.format(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d",
                        min.x(), min.y(), min.z(),
                        max.x(), max.y(), max.z(),
                        width, length, height, floorArea, wallArea, ceilingArea, volume);

        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(csv), null);
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                    "§cClipboard access failed (" + e.getClass().getSimpleName()
                            + "): " + e.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§a✓ Room information copied to clipboard."), false);
        return 1;
    }
}
