package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


 /** Suppresses vanilla footstep audio while Auroral Mirage or Veil Step is active.  */
@Mixin(Player::class)
abstract class VeilbornFootstepMixin {
    @Inject(method = ["playStepSound"], at = [At("HEAD")], cancellable = true) private fun `andromeda$hideVeilbornFootsteps`(pos: BlockPos, state: BlockState, ci: CallbackInfo) {
        if (VeilbornStealthCompat.suppressTraces(this as Any as Player)) {
            ci.cancel()
        }
    }
}
