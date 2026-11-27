package andromeda.origins.network

import andromeda.origins.AndromedaOrigins
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation


 /** Tiny S2C request used only to preserve /andromedaorigins config as a convenience opener.  */

class OpenConfigPayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<OpenConfigPayload> {
        return OpenConfigPayload.Companion.TYPE
    }
    companion object {
        val INSTANCE: OpenConfigPayload = OpenConfigPayload()
        val TYPE: CustomPacketPayload.Type<OpenConfigPayload> = CustomPacketPayload.Type<OpenConfigPayload>(
        ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "open_config"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, OpenConfigPayload> = object : StreamCodec<RegistryFriendlyByteBuf, OpenConfigPayload> {
            override fun decode(buf: RegistryFriendlyByteBuf): OpenConfigPayload {
                return OpenConfigPayload.Companion.INSTANCE
            }

            override fun encode(buf: RegistryFriendlyByteBuf, payload: OpenConfigPayload) {
            // No payload data; the packet itself is the request.
            }
        }

    }}
