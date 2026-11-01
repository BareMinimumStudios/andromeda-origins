package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat


 /** Suppresses vanilla status-effect swirl particles without changing or removing the effects themselves.  */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.LivingEntity::class)
abstract class VeilbornEffectParticleMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = ["tickEffects"], at = [org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V")]) private fun `andromeda$hideVeilbornEffectParticles`(
    level: net.minecraft.world.level.Level,
    particle: net.minecraft.core.particles.ParticleOptions,
    x: kotlin.Double,
    y: kotlin.Double,
    z: kotlin.Double,
    velocityX: kotlin.Double,
    velocityY: kotlin.Double,
    velocityZ: kotlin.Double,
    original: com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void>
    ) {
        val self: net.minecraft.world.entity.LivingEntity = this as kotlin.Any as net.minecraft.world.entity.LivingEntity
        if (!VeilbornStealthCompat.suppressTraces(self)) {
            original.call(level, particle, x, y, z, velocityX, velocityY, velocityZ)
        }
    }
}
