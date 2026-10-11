package andromeda.origins.mixin

import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.AndromedaConfigPolicy


 /** Ensures newly granted powers immediately respect the server config without revoking ownership.  */
@org.spongepowered.asm.mixin.Mixin(dev.overgrown.apoli.power.PowerContainerImpl::class)
abstract class PowerConfigSuppressionMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["addPower"], at = [org.spongepowered.asm.mixin.injection.At("RETURN")]) private fun `andromeda$applyConfigSuppression`(power: net.minecraft.resources.ResourceLocation, source: net.minecraft.resources.ResourceLocation,
    cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.Boolean>) {
        if (!cir.getReturnValue()) return
        val container: dev.overgrown.apoli.power.PowerContainer = this as kotlin.Any as dev.overgrown.apoli.power.PowerContainer
        val owner: net.minecraft.world.entity.Entity?
        try {
            owner = container.rawOwner()
        }catch (unattached: java.lang.IllegalStateException) {
            return
        }
        if (!AndromedaConfigRuntime.shouldSuppressPower(owner, power)) return
        container.suppressPower(power, AndromedaConfigPolicy.CONFIG_SUPPRESSION_SOURCE)
    }
}
