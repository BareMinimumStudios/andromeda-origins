package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat


 /** Suppresses vanilla footstep audio while Auroral Mirage or Veil Step is active.  */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.player.Player::class)
abstract class VeilbornFootstepMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["playStepSound"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun `andromeda$hideVeilbornFootsteps`(pos: net.minecraft.core.BlockPos, state: net.minecraft.world.level.block.state.BlockState, ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        if (VeilbornStealthCompat.suppressTraces(this as kotlin.Any as net.minecraft.world.entity.player.Player)) {
            ci.cancel()
        }
    }
}
