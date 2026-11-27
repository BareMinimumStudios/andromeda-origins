package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import dev.overgrown.apoli.power.PowerContainer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity


 /** Veilborn state checks used by Mirage stealth and Veil Step presentation/trace suppression.  */
object VeilbornStealthCompat {
    val MIRAGE_MARKER: ResourceLocation = VeilbornStealthCompat.id("veilborn/helper/veilpower")
    val VEIL_STEP_MARKER: ResourceLocation = VeilbornStealthCompat.id("veilborn/helper/veilstep")

    fun isAuroralMirage(entity: Entity?): Boolean {
        return VeilbornStealthCompat.hasActiveMarker(entity, VeilbornStealthCompat.MIRAGE_MARKER)
    }

    fun isVeilStep(entity: Entity?): Boolean {
        return VeilbornStealthCompat.hasActiveMarker(entity, VeilbornStealthCompat.VEIL_STEP_MARKER)
    }

    /** A Veil Step caster is a cloud to everyone; a caster also perceives living targets as clouds. */
    fun renderAsVeilCloud(entity: LivingEntity?, viewer: Entity?): Boolean {
        return entity != null && (isVeilStep(entity) || isVeilStep(viewer))
    }

    fun suppressTraces(entity: Entity?): Boolean {
        return VeilbornStealthCompat.isAuroralMirage(entity) || VeilbornStealthCompat.isVeilStep(entity)
    }

    private fun hasActiveMarker(entity: Entity?, power: ResourceLocation?): Boolean {
        if (entity == null) return false
        val container: PowerContainer? = PowerContainer.of(entity)
        return container != null && container.hasPower(power) && !container.isSuppressed(power)
    }

    private fun id(path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.CONTENT_NAMESPACE, path)
    }
}
