package andromeda.origins.platform

import andromeda.origins.platform.FabricVersionHandshake
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking.LoginQueryRequestHandler



 /** Client half of the exact Andromeda Origins login-version handshake.  */
object FabricVersionHandshakeClient {
    fun register() {
        ClientLoginNetworking.registerGlobalReceiver(FabricVersionHandshake.CHANNEL,
        LoginQueryRequestHandler { client: net.minecraft.client.Minecraft, handler: net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl, request: net.minecraft.network.FriendlyByteBuf, callbacksConsumer: java.util.function.Consumer<net.minecraft.network.PacketSendListener> ->
                // Consume the server's protocol/version fields so malformed queries do not silently
                // look valid. The server remains authoritative and performs the actual comparison.
        request.readVarInt()
        request.readUtf(128)
        val response: net.minecraft.network.FriendlyByteBuf = PacketByteBufs.create()
        response.writeVarInt(FabricVersionHandshake.PROTOCOL_VERSION)
        response.writeUtf(FabricVersionHandshake.currentVersion())
        java.util.concurrent.CompletableFuture.completedFuture<net.minecraft.network.FriendlyByteBuf>(response)
        })
    }
}