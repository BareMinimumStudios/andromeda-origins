package andromeda.origins.mixin

import andromeda.origins.client.FaerieJumpInput
import dev.overgrown.apoli.network.payload.PowerActivationC2S
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Connection::class)
abstract class FaerieJumpPacketMixin {
    @Inject(method = ["send(Lnet/minecraft/network/protocol/Packet;)V"], at = [At("HEAD")], cancellable = true)
    private fun `andromeda$keepGroundJump`(packet: Packet<*>, ci: CallbackInfo) {
        val payload = (packet as? ServerboundCustomPayloadPacket)?.payload() as? PowerActivationC2S ?: return
        if (FaerieJumpInput.blocks(payload.power())) ci.cancel()
    }
}
