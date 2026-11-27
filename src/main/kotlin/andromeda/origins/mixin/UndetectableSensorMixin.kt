package andromeda.origins.mixin

import andromeda.origins.compat.UndetectableCompat
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.sensing.Sensor
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


@Mixin(Sensor::class)
object UndetectableSensorMixin {
    @Inject(method = ["isEntityTargetable"
    , "isEntityAttackable"
    , "isEntityAttackableIgnoringLineOfSight"
    ], at = [At("HEAD")], cancellable = true) @JvmStatic private fun `andromeda$hideFromMobSensors`(observer: LivingEntity, target: LivingEntity, cir: CallbackInfoReturnable<Boolean>) {
        if (observer is Mob && UndetectableCompat.isUndetectable(target)) {
            cir.setReturnValue(false)
        }
    }
}
