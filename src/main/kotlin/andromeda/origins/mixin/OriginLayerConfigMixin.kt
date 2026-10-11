package andromeda.origins.mixin

import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.AndromedaConfigPolicy


 /**
 * Server/client-consistent filtering for the primary species layer. This intentionally operates
 * after Overgrown's datapack merge so mod resource-pack order cannot reintroduce stock Origins.
 */
@org.spongepowered.asm.mixin.Mixin(dev.overgrown.origins.origin.OriginLayer::class)
abstract class OriginLayerConfigMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["availableOrigins"], at = [org.spongepowered.asm.mixin.injection.At("RETURN")], cancellable = true) private fun `andromeda$filterAvailable`(player: net.minecraft.world.entity.player.Player, cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation>>) {
        val self: dev.overgrown.origins.origin.OriginLayer = this as kotlin.Any as dev.overgrown.origins.origin.OriginLayer
        if (AndromedaConfigPolicy.ORIGIN_LAYER != self.id()) return
        cir.setReturnValue(AndromedaConfigRuntime.filterPrimaryLayer(cir.getReturnValue()))
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = ["allOrigins"], at = [org.spongepowered.asm.mixin.injection.At("RETURN")], cancellable = true) private fun `andromeda$filterAll`(cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation>>) {
        val self: dev.overgrown.origins.origin.OriginLayer = this as kotlin.Any as dev.overgrown.origins.origin.OriginLayer
        if (AndromedaConfigPolicy.ORIGIN_LAYER != self.id()) return
        cir.setReturnValue(AndromedaConfigRuntime.filterPrimaryLayer(cir.getReturnValue()))
    }
}
