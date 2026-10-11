package andromeda.origins.network

import andromeda.origins.AndromedaOrigins


 /** Tiny S2C request used only to preserve /andromedaorigins config as a convenience opener.  */

class OpenConfigPayload : net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    override fun type(): net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<OpenConfigPayload> {
        return OpenConfigPayload.Companion.TYPE
    }
    companion object {
        val INSTANCE: OpenConfigPayload = OpenConfigPayload()
        val TYPE: net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<OpenConfigPayload> = net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<OpenConfigPayload>(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "open_config"))

        val CODEC: net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenConfigPayload> = object : net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenConfigPayload> {
            override fun decode(buf: net.minecraft.network.RegistryFriendlyByteBuf): OpenConfigPayload {
                return OpenConfigPayload.Companion.INSTANCE
            }

            override fun encode(buf: net.minecraft.network.RegistryFriendlyByteBuf, payload: OpenConfigPayload) {
            // No payload data; the packet itself is the request.
            }
        }

    }}
