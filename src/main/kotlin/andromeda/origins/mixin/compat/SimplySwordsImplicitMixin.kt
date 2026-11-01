package andromeda.origins.mixin.compat

import andromeda.origins.compat.WeaponBalanceCompat
import net.minecraft.network.chat.Component
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Pseudo
@Mixin(targets = ["net.sweenus.simplyswords.api.WeaponImplicitRegistry"], remap = false)
object SimplySwordsImplicitMixin {
    @JvmStatic
    @Inject(method = ["applyBleed"], at = [At("HEAD")], cancellable = true, remap = false, require = 0)
    private fun `andromeda$blockImplicitBleed`(ci: CallbackInfo) {
        if (WeaponBalanceCompat.blocksBleed()) ci.cancel()
    }

    @JvmStatic
    @Inject(method = ["formatTooltip*"], at = [At("RETURN")], cancellable = true, remap = false, require = 0)
    private fun `andromeda$implicitTooltip`(cir: CallbackInfoReturnable<Component?>) {
        cir.returnValue = WeaponBalanceCompat.tooltip(cir.returnValue)
    }
}
