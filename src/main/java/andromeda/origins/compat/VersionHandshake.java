package andromeda.origins.compat;

import andromeda.origins.AndromedaOrigins;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enforces an exact Andromeda Origins client/server version match during Fabric's login-query
 * stage. A client without the mod will not understand the query and is rejected before joining;
 * a client with a different Andromeda Origins version is rejected with both versions shown.
 */
public final class VersionHandshake {

    private static final Logger LOGGER = LoggerFactory.getLogger("Andromeda Origins/Version Sync");

    public static final Identifier CHANNEL = Identifier.of(AndromedaOrigins.MOD_ID, "version_check");
    public static final int PROTOCOL_VERSION = 1;
    private static final int MAX_VERSION_LENGTH = 128;

    private VersionHandshake() {}

    public static String currentVersion() {
        return FabricLoader.getInstance()
            .getModContainer(AndromedaOrigins.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }

    public static void registerServer() {
        ServerLoginNetworking.registerGlobalReceiver(CHANNEL,
            (server, handler, understood, buf, synchronizer, responseSender) -> {
                String serverVersion = currentVersion();

                if (!understood) {
                    LOGGER.warn("Rejected a client that does not have Andromeda Origins {} installed.", serverVersion);
                    handler.disconnect(Text.literal(
                        "Andromeda Origins is required by this server.\n"
                            + "Install Andromeda Origins " + serverVersion + " and reconnect."
                    ));
                    return;
                }

                try {
                    int clientProtocol = buf.readVarInt();
                    String clientVersion = buf.readString(MAX_VERSION_LENGTH);

                    if (clientProtocol != PROTOCOL_VERSION || !serverVersion.equals(clientVersion)) {
                        LOGGER.warn("Rejected Andromeda Origins mismatch: server={}, client={}, protocol={}/{}.",
                            serverVersion, clientVersion, PROTOCOL_VERSION, clientProtocol);

                        handler.disconnect(Text.literal(
                            "Andromeda Origins version mismatch.\n"
                                + "Server: " + serverVersion + "\n"
                                + "Client: " + clientVersion + "\n"
                                + "Install the exact same Andromeda Origins version as the server."
                        ));
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("Rejected a malformed Andromeda Origins version response.", exception);
                    handler.disconnect(Text.literal(
                        "Could not verify your Andromeda Origins version.\n"
                            + "Server requires version " + serverVersion + "."
                    ));
                }
            });

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            PacketByteBuf request = PacketByteBufs.create();
            request.writeVarInt(PROTOCOL_VERSION);
            request.writeString(currentVersion());
            sender.sendPacket(CHANNEL, request);
        });
    }
}
