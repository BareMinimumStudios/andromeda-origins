package andromeda.origins.mixin

import andromeda.origins.compat.UndetectableCompat


@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.Mob::class)
abstract class UndetectableMobTargetMixin {
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = ["setTarget"], at = org.spongepowered.asm.mixin.injection.At("HEAD"), argsOnly = true) private fun `andromeda$denyUndetectableTarget`(target: net.minecraft.world.entity.LivingEntity?): net.minecraft.world.entity.LivingEntity? {
        return if (UndetectableCompat.isUndetectable(target)) null else target
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = ["getTarget"], at = [org.spongepowered.asm.mixin.injection.At("RETURN")], cancellable = true) private fun `andromeda$forgetUndetectableTarget`(cir: org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.entity.LivingEntity>) {
        if (UndetectableCompat.isUndetectable(cir.getReturnValue())) {
            cir.setReturnValue(null)
        }
    }
}
