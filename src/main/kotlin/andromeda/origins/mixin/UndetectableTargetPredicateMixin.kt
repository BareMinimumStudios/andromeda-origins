package andromeda.origins.mixin

import andromeda.origins.compat.UndetectableCompat
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


@Mixin(TargetingConditions::class)
abstract class UndetectableTargetPredicateMixin {
    @Inject(method = ["test"], at = [At("HEAD")], cancellable = true) private fun `andromeda$rejectUndetectableMobTarget`(baseEntity: LivingEntity, targetEntity: LivingEntity, cir: CallbackInfoReturnable<Boolean>) {
        if (baseEntity is Mob && UndetectableCompat.isUndetectable(targetEntity)) {
            cir.setReturnValue(false)
        }
    }
}
