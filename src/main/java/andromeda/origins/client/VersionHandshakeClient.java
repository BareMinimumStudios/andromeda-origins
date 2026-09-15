package andromeda.origins.client;

import andromeda.origins.compat.VersionHandshake;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.concurrent.CompletableFuture;

/** Client half of the exact Andromeda Origins login-version handshake. */
public final class VersionHandshakeClient {

    private VersionHandshakeClient() {}

    public static void register() {
        ClientLoginNetworking.registerGlobalReceiver(VersionHandshake.CHANNEL,
            (client, handler, request, callbacksConsumer) -> {
                // Consume the server's protocol/version fields so malformed queries do not silently
                // look valid. The server remains authoritative and performs the actual comparison.
                request.readVarInt();
                request.readString(128);

                PacketByteBuf response = PacketByteBufs.create();
                response.writeVarInt(VersionHandshake.PROTOCOL_VERSION);
                response.writeString(VersionHandshake.currentVersion());
                return CompletableFuture.completedFuture(response);
            });
    }
}
