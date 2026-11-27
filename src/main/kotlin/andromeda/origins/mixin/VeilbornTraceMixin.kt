package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


 /** Suppresses vanilla sprint particles during Mirage or Veil Step so authored Veil visuals stay clean.  */
@Mixin(Entity::class)
abstract class VeilbornTraceMixin {
    @Inject(method = ["canSpawnSprintParticle"], at = [At("HEAD")], cancellable = true) private fun `andromeda$hideVeilbornSprintParticles`(cir: CallbackInfoReturnable<Boolean>) {
        if (VeilbornStealthCompat.suppressTraces(this as Any as Entity)) {
            cir.setReturnValue(false)
        }
    }
}
