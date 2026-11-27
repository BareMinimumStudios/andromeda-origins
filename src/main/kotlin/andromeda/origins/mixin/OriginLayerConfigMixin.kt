package andromeda.origins.mixin

import andromeda.origins.config.AndromedaConfigPolicy
import andromeda.origins.config.AndromedaConfigRuntime
import dev.overgrown.origins.origin.OriginLayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


 /**
 * Server/client-consistent filtering for the primary species layer. This intentionally operates
 * after Overgrown's datapack merge so mod resource-pack order cannot reintroduce stock Origins.
 */
@Mixin(OriginLayer::class)
abstract class OriginLayerConfigMixin {
    @Inject(method = ["availableOrigins"], at = [At("RETURN")], cancellable = true) private fun `andromeda$filterAvailable`(player: Player, cir: CallbackInfoReturnable<MutableList<ResourceLocation>>) {
        val self: OriginLayer = this as Any as OriginLayer
        if (AndromedaConfigPolicy.ORIGIN_LAYER != self.id()) return
        cir.setReturnValue(AndromedaConfigRuntime.filterPrimaryLayer(cir.getReturnValue()))
    }

    @Inject(method = ["allOrigins"], at = [At("RETURN")], cancellable = true) private fun `andromeda$filterAll`(cir: CallbackInfoReturnable<MutableList<ResourceLocation>>) {
        val self: OriginLayer = this as Any as OriginLayer
        if (AndromedaConfigPolicy.ORIGIN_LAYER != self.id()) return
        cir.setReturnValue(AndromedaConfigRuntime.filterPrimaryLayer(cir.getReturnValue()))
    }
}
