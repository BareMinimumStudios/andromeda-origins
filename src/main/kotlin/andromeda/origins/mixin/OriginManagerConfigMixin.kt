package andromeda.origins.mixin

import andromeda.origins.config.AndromedaConfigApplier
import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.AndromedaConfigPolicy


 /** Server-side backstop so a disabled primary-layer Origin cannot be selected by a crafted packet.  */
@org.spongepowered.asm.mixin.Mixin(dev.overgrown.origins.origin.OriginManager::class)
object OriginManagerConfigMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["availableTo"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) @JvmStatic private fun `andromeda$rejectDisabledOrigin`(player: net.minecraft.world.entity.player.Player, layerId: net.minecraft.resources.ResourceLocation,
    originId: net.minecraft.resources.ResourceLocation,
    cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.Boolean>) {
        if (AndromedaConfigPolicy.ORIGIN_LAYER == layerId
        && !AndromedaConfigRuntime.originEnabled(originId)) {
            cir.setReturnValue(false)
        }
    }

     /** Re-evaluate per-Origin shared power toggles after every successful Origin choice.  */
    @org.spongepowered.asm.mixin.injection.Inject(method = ["chooseOrigin"], at = [org.spongepowered.asm.mixin.injection.At("RETURN")]) @JvmStatic private fun `andromeda$refreshPowerSuppression`(player: net.minecraft.server.level.ServerPlayer, layerId: net.minecraft.resources.ResourceLocation,
    originId: net.minecraft.resources.ResourceLocation, fromOrb: kotlin.Boolean,
    ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo?) {
        if (AndromedaConfigPolicy.ORIGIN_LAYER == layerId) {
            AndromedaConfigApplier.applyPowerSuppression(player)
        }
    }
}
