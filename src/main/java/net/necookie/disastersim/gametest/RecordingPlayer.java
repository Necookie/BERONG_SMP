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
    /** The mock network channel; outbound packets (e.g. HUD payloads) can be read from it. */
    public EmbeddedChannel channel;

    private RecordingPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation info) {
        super(server, level, profile, info);
    }

    public static RecordingPlayer join(GameTestHelper helper, String name) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        RecordingPlayer player = new RecordingPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(PacketFlow.SERVERBOUND);
        player.channel = new EmbeddedChannel(connection);
        // Act as a fully negotiated NeoForge client so the mod's own payloads (HUD, shake, …) can be sent.
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        // The test server's default game mode is CREATIVE, whose invulnerability also keeps fire from
        // sticking. Students play in survival, so the mock player must too.
        player.setGameMode(GameType.SURVIVAL);
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

    /** Drains packets written to this player and returns the mod payloads among them. */
    public List<net.minecraft.network.protocol.common.custom.CustomPacketPayload> drainPayloads() {
        List<net.minecraft.network.protocol.common.custom.CustomPacketPayload> out = new java.util.ArrayList<>();
        Object msg;
        while ((msg = channel.readOutbound()) != null) collect(msg, out);
        return out;
    }

    private static void collect(Object msg, List<net.minecraft.network.protocol.common.custom.CustomPacketPayload> out) {
        if (msg instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket p) {
            out.add(p.payload());
        } else if (msg instanceof net.minecraft.network.protocol.BundlePacket<?> b) {
            for (Object sub : b.subPackets()) collect(sub, out);
        }
    }

    public boolean heard(String fragment) {
        return messages.stream().anyMatch(m -> m.contains(fragment));
    }

    public void leave() {
        ((ServerLevel) level()).getServer().getPlayerList().remove(this);
    }
}
