package andromeda.origins.mixin

import andromeda.origins.client.FaerieJumpInput
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Minecraft::class)
abstract class FaerieJumpInputMixin {
    @Inject(method = ["tick"], at = [At("HEAD")])
    private fun `andromeda$captureGroundJump`(ci: CallbackInfo) {
        FaerieJumpInput.beginTick(this as Any as Minecraft)
    }
}
