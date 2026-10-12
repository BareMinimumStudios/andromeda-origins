package andromeda.origins.mixin

import andromeda.origins.compat.NereidGills
import dev.overgrown.apoli.power.builtin.WaterBreathingPower
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Keep native client air prediction; the server performs this drain after player updates. */
@Mixin(WaterBreathingPower::class, remap = false)
object NereidGillsPowerMixin {
    @JvmStatic
    @Inject(method = ["tick"], at = [At("HEAD")], cancellable = true, remap = false)
    private fun `andromeda$serverGills`(entity: LivingEntity, ci: CallbackInfo) {
        if (entity is ServerPlayer && NereidGills.isDry(entity)) ci.cancel()
    }
}
