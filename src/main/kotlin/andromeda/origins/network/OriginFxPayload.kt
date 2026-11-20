package andromeda.origins.network
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

data class OriginFxPayload(val family: Int, val actor: Int, val at: Vec3, val color: Int, val radius: Float, val mode: Int, val detail: Int) : CustomPacketPayload {
    override fun type() = TYPE
    companion object {
        val TYPE = CustomPacketPayload.Type<OriginFxPayload>(ResourceLocation.parse("andromeda_origins:origin_contours"))
        val CODEC = object : StreamCodec<RegistryFriendlyByteBuf, OriginFxPayload> {
            override fun encode(b: RegistryFriendlyByteBuf, p: OriginFxPayload) { b.writeVarInt(p.family);b.writeVarInt(p.actor);b.writeDouble(p.at.x);b.writeDouble(p.at.y);b.writeDouble(p.at.z);b.writeInt(p.color);b.writeFloat(p.radius);b.writeVarInt(p.mode);b.writeVarInt(p.detail) }
            override fun decode(b: RegistryFriendlyByteBuf) = OriginFxPayload(b.readVarInt(),b.readVarInt(),Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readInt(),b.readFloat(),b.readVarInt(),b.readVarInt())
        }
    }
}
