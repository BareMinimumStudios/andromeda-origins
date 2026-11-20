package andromeda.origins.compat

import andromeda.origins.config.AndromedaConfigBootstrap
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

object DawnbreakerPresentation {
    enum class Preset { MINIMAL, STANDARD, CINEMATIC }
    fun settings() = AndromedaConfigBootstrap.config().championOrigins.dawnbreaker
    fun current() = settings().presentationPreset.get()
    fun tier(entity: LivingEntity): Int = PowerContainer.of(entity)?.let {
        PowerResources.read(it, ResourceLocation.parse("andromeda_origins:dawnbreaker/presentation_tier")).orElse(current().ordinal.toDouble()).toInt().coerceIn(0, 2)
    } ?: 1
    fun particles(base: Int) = (base * when (current()) { Preset.MINIMAL -> .4; Preset.STANDARD -> 1.0; Preset.CINEMATIC -> 1.25 }).toInt().coerceAtLeast(3)
    fun soundScale(tier: Int = current().ordinal) = when (tier) { 0 -> .5f; 2 -> 1.1f; else -> 1f }
}
