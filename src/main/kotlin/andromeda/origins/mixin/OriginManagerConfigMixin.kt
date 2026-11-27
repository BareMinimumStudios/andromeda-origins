package andromeda.origins.mixin

import andromeda.origins.config.AndromedaConfigApplier
import andromeda.origins.config.AndromedaConfigPolicy
import andromeda.origins.config.AndromedaConfigRuntime
import dev.overgrown.origins.origin.OriginManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


 /** Server-side backstop so a disabled primary-layer Origin cannot be selected by a crafted packet.  */
@Mixin(OriginManager::class)
object OriginManagerConfigMixin {
    @Inject(method = ["availableTo"], at = [At("HEAD")], cancellable = true) @JvmStatic private fun `andromeda$rejectDisabledOrigin`(player: Player, layerId: ResourceLocation,
    originId: ResourceLocation,
    cir: CallbackInfoReturnable<Boolean>) {
        if (AndromedaConfigPolicy.ORIGIN_LAYER == layerId
        && !AndromedaConfigRuntime.originEnabled(originId)) {
            cir.setReturnValue(false)
        }
    }

     /** Re-evaluate per-Origin shared power toggles after every successful Origin choice.  */
    @Inject(method = ["chooseOrigin"], at = [At("RETURN")]) @JvmStatic private fun `andromeda$refreshPowerSuppression`(player: ServerPlayer, layerId: ResourceLocation,
    originId: ResourceLocation, fromOrb: Boolean,
    ci: CallbackInfo?) {
        if (AndromedaConfigPolicy.ORIGIN_LAYER == layerId) {
            AndromedaConfigApplier.applyPowerSuppression(player)
        }
    }
}
