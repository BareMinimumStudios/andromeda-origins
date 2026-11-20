package andromeda.origins.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

/** Server-to-client cosmetic cue only. No client input, arbitrary assets or gameplay actions. */
data class DawnbreakerFxPayload(val kind: Int, val actor: Int, val position: Vec3, val flags: Int) : CustomPacketPayload {
    override fun type() = TYPE
    companion object {
        val TYPE = CustomPacketPayload.Type<DawnbreakerFxPayload>(ResourceLocation.parse("andromeda_origins:dawnbreaker_fx"))
        val CODEC = object : StreamCodec<RegistryFriendlyByteBuf, DawnbreakerFxPayload> {
            override fun encode(buf: RegistryFriendlyByteBuf, value: DawnbreakerFxPayload) {
                buf.writeVarInt(value.kind); buf.writeVarInt(value.actor); buf.writeVarInt(value.flags)
                buf.writeDouble(value.position.x); buf.writeDouble(value.position.y); buf.writeDouble(value.position.z)
            }
            override fun decode(buf: RegistryFriendlyByteBuf): DawnbreakerFxPayload {
                val kind = buf.readVarInt(); val actor = buf.readVarInt(); val flags = buf.readVarInt()
                return DawnbreakerFxPayload(kind, actor, Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), flags)
            }
        }
    }
}
