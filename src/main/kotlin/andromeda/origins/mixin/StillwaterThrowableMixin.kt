package andromeda.origins.mixin

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.projectile.ThrowableProjectile::class)
abstract class StillwaterThrowableMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = ["tick"], at = [org.spongepowered.asm.mixin.injection.At("HEAD")], cancellable = true)
    private fun pauseHeld(ci: org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
        if (andromeda.origins.compat.SovereignArts.isFrozen(this as Any as net.minecraft.world.entity.projectile.Projectile)) ci.cancel()
    }
}
