package andromeda.origins.mixin

import andromeda.origins.compat.SovereignArts
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ThrowableProjectile
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ThrowableProjectile::class)
abstract class StillwaterThrowableMixin {
    @Inject(method = ["tick"], at = [At("HEAD")], cancellable = true)
    private fun pauseHeld(ci: CallbackInfo) {
        if (SovereignArts.isFrozen(this as Any as Projectile)) ci.cancel()
    }
}
