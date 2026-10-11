package andromeda.origins.platform

import andromeda.origins.AndromedaOrigins
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginQueryResponseHandler
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginSynchronizer
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs


 /**
 * Enforces an exact Andromeda Origins client/server version match during Fabric's login-query
 * stage. A client without the mod will not understand the query and is rejected before joining;
 * a client with a different Andromeda Origins version is rejected with both versions shown.
 */
object FabricVersionHandshake {
    val LOGGER: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("Andromeda Origins/Version Sync")

    val CHANNEL: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "version_check")
    const val PROTOCOL_VERSION: kotlin.Int = 3
    private const val MAX_VERSION_LENGTH: kotlin.Int = 128

    fun currentVersion(): kotlin.String {
        return net.fabricmc.loader.api.FabricLoader.getInstance()
        .getModContainer(AndromedaOrigins.MOD_ID)
        .map<kotlin.String>(java.util.function.Function { container: net.fabricmc.loader.api.ModContainer -> container.getMetadata().getVersion().getFriendlyString()})
        .orElse("unknown")
    }

    fun registerServer() {
        ServerLoginNetworking.registerGlobalReceiver(FabricVersionHandshake.CHANNEL,
        LoginQueryResponseHandler callback@ { server: net.minecraft.server.MinecraftServer, handler: net.minecraft.server.network.ServerLoginPacketListenerImpl, understood: kotlin.Boolean, buf: net.minecraft.network.FriendlyByteBuf, synchronizer: LoginSynchronizer, responseSender: PacketSender -> val serverVersion: kotlin.String = FabricVersionHandshake.currentVersion()

        if (!understood) {
            FabricVersionHandshake.LOGGER.warn("Rejected a client that does not have Andromeda Origins {} installed.", serverVersion)
            handler.disconnect(net.minecraft.network.chat.Component.literal(
            ("Andromeda Origins is required by this server.\n"
            + "Install Andromeda Origins " + serverVersion + " and reconnect.")
            ))
            return@callback
        }

        try {
            val clientProtocol: kotlin.Int = buf.readVarInt()
            val clientVersion: kotlin.String = buf.readUtf(FabricVersionHandshake.MAX_VERSION_LENGTH)

            if (clientProtocol != FabricVersionHandshake.PROTOCOL_VERSION || serverVersion != clientVersion) {
                FabricVersionHandshake.LOGGER.warn("Rejected Andromeda Origins mismatch: server={}, client={}, protocol={}/{}.",
                serverVersion, clientVersion, FabricVersionHandshake.PROTOCOL_VERSION, clientProtocol)

                handler.disconnect(net.minecraft.network.chat.Component.literal(
                ("Andromeda Origins version mismatch.\n"
                + "Server: " + serverVersion + "\n"
                + "Client: " + clientVersion + "\n"
                + "Install the exact same Andromeda Origins version as the server.")
                ))
            }
        }catch (exception: java.lang.RuntimeException) {
            FabricVersionHandshake.LOGGER.warn("Rejected a malformed Andromeda Origins version response.", exception)
            handler.disconnect(net.minecraft.network.chat.Component.literal(
            ("Could not verify your Andromeda Origins version.\n"
            + "Server requires version " + serverVersion + ".")
            ))
        }

        })

        ServerLoginConnectionEvents.QUERY_START.register(net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents.QueryStart { handler: net.minecraft.server.network.ServerLoginPacketListenerImpl, server: net.minecraft.server.MinecraftServer, sender: LoginPacketSender, synchronizer: LoginSynchronizer -> val request: net.minecraft.network.FriendlyByteBuf = PacketByteBufs.create()
        request.writeVarInt(FabricVersionHandshake.PROTOCOL_VERSION)
        request.writeUtf(FabricVersionHandshake.currentVersion())
        sender.sendPacket(FabricVersionHandshake.CHANNEL, request)

        })
    }
}