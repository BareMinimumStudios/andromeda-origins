package andromeda.origins.mixin;

import andromeda.origins.compat.UndetectableCompat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class UndetectableMobTargetMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity andromeda$denyUndetectableTarget(LivingEntity target) {
        return UndetectableCompat.isUndetectable(target) ? null : target;
    }

    @Inject(method = "getTarget", at = @At("RETURN"), cancellable = true)
    private void andromeda$forgetUndetectableTarget(CallbackInfoReturnable<LivingEntity> cir) {
        if (UndetectableCompat.isUndetectable(cir.getReturnValue())) {
            cir.setReturnValue(null);
        }
    }
}
