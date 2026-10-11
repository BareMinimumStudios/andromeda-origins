package andromeda.origins.mixin

import andromeda.origins.compat.UndetectableCompat


@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.ai.targeting.TargetingConditions::class)
abstract class UndetectableTargetPredicateMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["test"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true) private fun `andromeda$rejectUndetectableMobTarget`(baseEntity: net.minecraft.world.entity.LivingEntity, targetEntity: net.minecraft.world.entity.LivingEntity, cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<kotlin.Boolean>) {
        if (baseEntity is net.minecraft.world.entity.Mob && UndetectableCompat.isUndetectable(targetEntity)) {
            cir.setReturnValue(false)
        }
    }
}
