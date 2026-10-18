package net.necookie.disastersim.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A mock server player for game tests that joins the real player list (so the mod's login
 * handlers, sessions and commands all see it) and records every system chat line it receives,
 * so tests can assert on what a student would actually have been told.
 * Same construction as vanilla's {@code GameTestHelper.makeMockServerPlayerInLevel}.
 */
public class RecordingPlayer extends ServerPlayer {

    public final List<String> messages = new CopyOnWriteArrayList<>();

    private RecordingPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation info) {
        super(server, level, profile, info);
    }

    public static RecordingPlayer join(GameTestHelper helper, String name) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        RecordingPlayer player = new RecordingPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // Act as a fully negotiated NeoForge client so the mod's own payloads (HUD, shake, …) can be sent.
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    @Override
    public GameType gameMode() {
        return GameType.SURVIVAL;
    }

    @Override
    public void sendSystemMessage(Component message, boolean overlay) {
        messages.add(message.getString());
        super.sendSystemMessage(message, overlay);
    }

    public boolean heard(String fragment) {
        return messages.stream().anyMatch(m -> m.contains(fragment));
    }

    public void leave() {
        ((ServerLevel) level()).getServer().getPlayerList().remove(this);
    }
}
