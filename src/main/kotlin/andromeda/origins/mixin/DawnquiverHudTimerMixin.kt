package andromeda.origins.mixin

import andromeda.origins.client.DawnquiverClientCompat
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Scoped only to our synchronous call into the installed Dawnquiver HUD. Does not mutate item-use state. */
@Mixin(LivingEntity::class)
abstract class DawnquiverHudTimerMixin {
    @Inject(method = ["getUseItemRemainingTicks"], at = [At("HEAD")], cancellable = true)
    private fun renderDawnTimer(ci: CallbackInfoReturnable<Int>) {
        DawnquiverClientCompat.remainingForHud()?.let { ci.returnValue = it }
    }
}
