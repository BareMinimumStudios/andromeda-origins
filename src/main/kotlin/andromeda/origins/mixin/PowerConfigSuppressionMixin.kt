package andromeda.origins.mixin

import andromeda.origins.compat.AndromedaAttributeCleanup
import andromeda.origins.config.AndromedaConfigPolicy
import andromeda.origins.config.AndromedaConfigRuntime
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerContainerImpl
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


 /** Ensures newly granted powers immediately respect the server config without revoking ownership.  */
@Mixin(PowerContainerImpl::class)
abstract class PowerConfigSuppressionMixin {
    @Inject(method = ["addPower"], at = [At("RETURN")]) private fun `andromeda$applyConfigSuppression`(power: ResourceLocation, source: ResourceLocation,
    cir: CallbackInfoReturnable<Boolean>) {
        if (!cir.getReturnValue()) return
        val container: PowerContainer = this as Any as PowerContainer
        val owner: Entity?
        try {
            owner = container.rawOwner()
        }catch (unattached: IllegalStateException) {
            return
        }
        if (!AndromedaConfigRuntime.shouldSuppressPower(owner, power)) return
        container.suppressPower(power, AndromedaConfigPolicy.CONFIG_SUPPRESSION_SOURCE)
    }
    @Inject(method = ["removePower"], at = [At("RETURN")])
    private fun `andromeda$cleanRemovedAttribute`(power: ResourceLocation, source: ResourceLocation,
        cir: CallbackInfoReturnable<Boolean>) {
        if (cir.returnValue) AndromedaAttributeCleanup.afterRemoval(
            this as Any as PowerContainer, power)
    }

}
