package andromeda.origins.mixin

import andromeda.origins.compat.UndetectableCompat
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


@Mixin(Mob::class)
abstract class UndetectableMobTargetMixin {
    @WrapMethod(method = ["setTarget"])
    private fun `andromeda$denyUndetectableTarget`(target: LivingEntity?,
        original: Operation<Void>) {
        original.call(if (UndetectableCompat.isUndetectable(target)) null else target)
    }

    @Inject(method = ["getTarget"], at = [At("RETURN")], cancellable = true) private fun `andromeda$forgetUndetectableTarget`(cir: CallbackInfoReturnable<LivingEntity>) {
        if (UndetectableCompat.isUndetectable(cir.getReturnValue())) {
            cir.setReturnValue(null)
        }
    }
}
