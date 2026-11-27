package andromeda.origins.platform

import andromeda.origins.platform.FabricVersionHandshake
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking.LoginQueryRequestHandler
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.PacketSendListener



 /** Client half of the exact Andromeda Origins login-version handshake.  */
object FabricVersionHandshakeClient {
    fun register() {
        ClientLoginNetworking.registerGlobalReceiver(FabricVersionHandshake.CHANNEL,
        LoginQueryRequestHandler { client: Minecraft, handler: ClientHandshakePacketListenerImpl, request: FriendlyByteBuf, callbacksConsumer: Consumer<PacketSendListener> ->
                // Consume the server's protocol/version fields so malformed queries do not silently
                // look valid. The server remains authoritative and performs the actual comparison.
        request.readVarInt()
        request.readUtf(128)
        val response: FriendlyByteBuf = PacketByteBufs.create()
        response.writeVarInt(FabricVersionHandshake.PROTOCOL_VERSION)
        response.writeUtf(FabricVersionHandshake.currentVersion())
        CompletableFuture.completedFuture<FriendlyByteBuf>(response)
        })
    }
}