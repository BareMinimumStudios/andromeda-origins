package andromeda.origins.mixin;

import andromeda.origins.compat.UndetectableCompat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sensor.class)
public abstract class UndetectableSensorMixin {
    @Inject(
        method = {
            "testTargetPredicate",
            "testAttackableTargetPredicate",
            "testAttackableTargetPredicateIgnoreVisibility"
        },
        at = @At("HEAD"),
        cancellable = true
    )
    private static void andromeda$hideFromMobSensors(LivingEntity observer, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (observer instanceof MobEntity && UndetectableCompat.isUndetectable(target)) {
            cir.setReturnValue(false);
        }
    }
}
