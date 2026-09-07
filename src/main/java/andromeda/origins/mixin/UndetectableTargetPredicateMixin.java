package andromeda.origins.mixin;

import andromeda.origins.compat.UndetectableCompat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetPredicate.class)
public abstract class UndetectableTargetPredicateMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void andromeda$rejectUndetectableMobTarget(LivingEntity baseEntity, LivingEntity targetEntity, CallbackInfoReturnable<Boolean> cir) {
        if (baseEntity instanceof MobEntity && UndetectableCompat.isUndetectable(targetEntity)) {
            cir.setReturnValue(false);
        }
    }
}
