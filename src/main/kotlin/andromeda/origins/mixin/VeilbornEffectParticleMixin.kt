package andromeda.origins.mixin

import andromeda.origins.compat.VeilbornStealthCompat
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At


 /** Suppresses vanilla status-effect swirl particles without changing or removing the effects themselves.  */
@Mixin(LivingEntity::class)
abstract class VeilbornEffectParticleMixin {
    @WrapOperation(method = ["tickEffects"], at = [At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V")]) private fun `andromeda$hideVeilbornEffectParticles`(
    level: Level,
    particle: ParticleOptions,
    x: Double,
    y: Double,
    z: Double,
    velocityX: Double,
    velocityY: Double,
    velocityZ: Double,
    original: Operation<Void>
    ) {
        val self: LivingEntity = this as Any as LivingEntity
        if (!VeilbornStealthCompat.suppressTraces(self)) {
            original.call(level, particle, x, y, z, velocityX, velocityY, velocityZ)
        }
    }
}
