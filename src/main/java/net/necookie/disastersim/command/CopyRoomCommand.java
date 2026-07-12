package net.necookie.disastersim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * {@code //copyroom [name...]} — reads the caller's active WorldEdit cuboid selection, computes
 * room dimensions/areas/volume, and copies a human-readable summary to the server machine's OS
 * clipboard. The optional trailing {@code name} (e.g. {@code //copyroom Room 201}) is echoed back
 * as a label line, for surveying a building room-by-room the same way {@code SimRoom}'s
 * {@code CCS_UPPER_ROOMS}/{@code CCS_GROUND_ROOMS} tables in CLAUDE.md were built.
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
                .executes(ctx -> run(ctx, null))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "name")))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, String roomName) {
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

        String summary = formatRoomSummary(roomName, min, max, width, length, height,
                floorArea, wallArea, ceilingArea, volume);

        try {
            copyToClipboard(summary);
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                    "§cClipboard access failed (" + e.getClass().getSimpleName()
                            + "): " + e.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§a✓ Room information copied to clipboard."), false);
        return 1;
    }

    private static String formatRoomSummary(String roomName, BlockVector3 min, BlockVector3 max,
            int width, int length, int height,
            long floorArea, long wallArea, long ceilingArea, long volume) {
        StringBuilder sb = new StringBuilder();
        if (roomName != null && !roomName.isBlank()) {
            sb.append(roomName.trim()).append(System.lineSeparator());
        }
        sb.append(String.format(Locale.ROOT, "Corner 1: (%d, %d, %d)%n", min.x(), min.y(), min.z()));
        sb.append(String.format(Locale.ROOT, "Corner 2: (%d, %d, %d)%n%n", max.x(), max.y(), max.z()));
        sb.append(String.format(Locale.ROOT, "%-7s %3d%n", "Width:", width));
        sb.append(String.format(Locale.ROOT, "%-7s %3d%n", "Length:", length));
        sb.append(String.format(Locale.ROOT, "%-7s %3d%n%n", "Height:", height));
        sb.append(String.format(Locale.ROOT, "%-13s %5d%n", "Floor Area:", floorArea));
        sb.append(String.format(Locale.ROOT, "%-13s %5d%n", "Wall Area:", wallArea));
        sb.append(String.format(Locale.ROOT, "%-13s %5d%n", "Ceiling Area:", ceilingArea));
        sb.append(String.format(Locale.ROOT, "%-13s %5d", "Volume:", volume));
        return sb.toString();
    }

    /**
     * NeoForge/FML sets {@code java.awt.headless=true} at bootstrap (both client and dedicated
     * server JVMs — it keeps AWT from fighting LWJGL's native window), so {@link Toolkit}'s
     * clipboard always throws {@link HeadlessException} inside a running Minecraft process. Fall
     * back to shelling out to the OS's own clipboard utility, which doesn't care about AWT state.
     */
    private static void copyToClipboard(String text) throws Exception {
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(text), null);
                return;
            } catch (HeadlessException ignored) {
                // Fall through to the native-command path below.
            }
        }

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[] command;
        if (osName.contains("win")) {
            command = new String[] {"clip"};
        } else if (osName.contains("mac")) {
            command = new String[] {"pbcopy"};
        } else {
            command = new String[] {"xclip", "-selection", "clipboard"};
        }

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (OutputStream out = process.getOutputStream()) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Clipboard command '" + command[0] + "' timed out");
        }
        if (process.exitValue() != 0) {
            throw new IOException("Clipboard command '" + command[0] + "' exited with code " + process.exitValue());
        }
    }
}
