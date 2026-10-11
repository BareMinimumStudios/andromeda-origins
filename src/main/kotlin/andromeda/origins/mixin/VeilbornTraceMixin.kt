package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat


 /** Suppresses vanilla sprint particles during Mirage or Veil Step so authored Veil visuals stay clean.  */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.Entity::class)
abstract class VeilbornTraceMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["canSpawnSprintParticle"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun `andromeda$hideVeilbornSprintParticles`(cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.Boolean>) {
        if (VeilbornStealthCompat.suppressTraces(this as kotlin.Any as net.minecraft.world.entity.Entity)) {
            cir.setReturnValue(false)
        }
    }
}
