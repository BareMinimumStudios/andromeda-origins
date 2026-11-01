package andromeda.origins.mixin.compat

import andromeda.origins.compat.WeaponBalanceCompat
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Pseudo
@Mixin(targets = ["net.rosemarythyme.simplymore.registry.item.ImplicitRegistry"], remap = false)
object SimplyMoreImplicitMixin {
    @JvmStatic
    @Inject(method = ["fatigue"], at = [At("HEAD")], cancellable = true, remap = false, require = 0)
    private fun `andromeda$blockImplicitFatigue`(ci: CallbackInfo) {
        if (WeaponBalanceCompat.blocksFatigue()) ci.cancel()
    }

    @JvmStatic
    @Inject(method = ["stun"], at = [At("HEAD")], cancellable = true, remap = false, require = 0)
    private fun `andromeda$blockImplicitStun`(ci: CallbackInfo) {
        if (WeaponBalanceCompat.blocksStun()) ci.cancel()
    }
}
