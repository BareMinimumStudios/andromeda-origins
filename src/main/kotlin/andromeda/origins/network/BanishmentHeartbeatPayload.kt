package andromeda.origins.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Recipient-only fixed cue: no coordinates, arbitrary sound IDs or client-to-server action. */
class BanishmentHeartbeatPayload : CustomPacketPayload {
    override fun type() = TYPE
    companion object {
        val INSTANCE = BanishmentHeartbeatPayload()
        val TYPE = CustomPacketPayload.Type<BanishmentHeartbeatPayload>(ResourceLocation.parse("andromeda_origins:banishment_heartbeat"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BanishmentHeartbeatPayload> = StreamCodec.unit(INSTANCE)
    }
}
