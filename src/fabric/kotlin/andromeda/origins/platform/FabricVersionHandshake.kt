package andromeda.origins.platform

import andromeda.origins.AndromedaOrigins
import java.util.function.Function
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginQueryResponseHandler
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginSynchronizer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerLoginPacketListenerImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory


 /**
 * Enforces an exact Andromeda Origins client/server version match during Fabric's login-query
 * stage. A client without the mod will not understand the query and is rejected before joining;
 * a client with a different Andromeda Origins version is rejected with both versions shown.
 */
object FabricVersionHandshake {
    val LOGGER: Logger = LoggerFactory.getLogger("Andromeda Origins/Version Sync")

    val CHANNEL: ResourceLocation = ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "version_check")
    const val PROTOCOL_VERSION: Int = 3
    private const val MAX_VERSION_LENGTH: Int = 128

    fun currentVersion(): String {
        return FabricLoader.getInstance()
        .getModContainer(AndromedaOrigins.MOD_ID)
        .map<String>(Function { container: ModContainer -> container.getMetadata().getVersion().getFriendlyString()})
        .orElse("unknown")
    }

    fun registerServer() {
        ServerLoginNetworking.registerGlobalReceiver(FabricVersionHandshake.CHANNEL,
        LoginQueryResponseHandler callback@ { server: MinecraftServer, handler: ServerLoginPacketListenerImpl, understood: Boolean, buf: FriendlyByteBuf, synchronizer: LoginSynchronizer, responseSender: PacketSender -> val serverVersion: String = FabricVersionHandshake.currentVersion()

        if (!understood) {
            FabricVersionHandshake.LOGGER.warn("Rejected a client that does not have Andromeda Origins {} installed.", serverVersion)
            handler.disconnect(Component.literal(
            ("Andromeda Origins is required by this server.\n"
            + "Install Andromeda Origins " + serverVersion + " and reconnect.")
            ))
            return@callback
        }

        try {
            val clientProtocol: Int = buf.readVarInt()
            val clientVersion: String = buf.readUtf(FabricVersionHandshake.MAX_VERSION_LENGTH)

            if (clientProtocol != FabricVersionHandshake.PROTOCOL_VERSION || serverVersion != clientVersion) {
                FabricVersionHandshake.LOGGER.warn("Rejected Andromeda Origins mismatch: server={}, client={}, protocol={}/{}.",
                serverVersion, clientVersion, FabricVersionHandshake.PROTOCOL_VERSION, clientProtocol)

                handler.disconnect(Component.literal(
                ("Andromeda Origins version mismatch.\n"
                + "Server: " + serverVersion + "\n"
                + "Client: " + clientVersion + "\n"
                + "Install the exact same Andromeda Origins version as the server.")
                ))
            }
        }catch (exception: RuntimeException) {
            FabricVersionHandshake.LOGGER.warn("Rejected a malformed Andromeda Origins version response.", exception)
            handler.disconnect(Component.literal(
            ("Could not verify your Andromeda Origins version.\n"
            + "Server requires version " + serverVersion + ".")
            ))
        }

        })

        ServerLoginConnectionEvents.QUERY_START.register(ServerLoginConnectionEvents.QueryStart { handler: ServerLoginPacketListenerImpl, server: MinecraftServer, sender: LoginPacketSender, synchronizer: LoginSynchronizer -> val request: FriendlyByteBuf = PacketByteBufs.create()
        request.writeVarInt(FabricVersionHandshake.PROTOCOL_VERSION)
        request.writeUtf(FabricVersionHandshake.currentVersion())
        sender.sendPacket(FabricVersionHandshake.CHANNEL, request)

        })
    }
}