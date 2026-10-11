package andromeda.origins.mixin

import andromeda.origins.compat.UndetectableCompat


@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.ai.sensing.Sensor::class)
object UndetectableSensorMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["isEntityTargetable"
    , "isEntityAttackable"
    , "isEntityAttackableIgnoringLineOfSight"
    ], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) @JvmStatic private fun `andromeda$hideFromMobSensors`(observer: net.minecraft.world.entity.LivingEntity, target: net.minecraft.world.entity.LivingEntity, cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.Boolean>) {
        if (observer is net.minecraft.world.entity.Mob && UndetectableCompat.isUndetectable(target)) {
            cir.setReturnValue(false)
        }
    }
}
