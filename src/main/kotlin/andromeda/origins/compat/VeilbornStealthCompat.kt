package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins


 /** Veilborn state checks used by Mirage stealth and Veil Step presentation/trace suppression.  */
object VeilbornStealthCompat {
    val MIRAGE_MARKER: net.minecraft.resources.ResourceLocation = VeilbornStealthCompat.id("veilborn/helper/veilpower")
    val VEIL_STEP_MARKER: net.minecraft.resources.ResourceLocation = VeilbornStealthCompat.id("veilborn/helper/veilstep")

    fun isAuroralMirage(entity: net.minecraft.world.entity.Entity?): kotlin.Boolean {
        return VeilbornStealthCompat.hasActiveMarker(entity, VeilbornStealthCompat.MIRAGE_MARKER)
    }

    fun isVeilStep(entity: net.minecraft.world.entity.Entity?): kotlin.Boolean {
        return VeilbornStealthCompat.hasActiveMarker(entity, VeilbornStealthCompat.VEIL_STEP_MARKER)
    }

    /** A Veil Step caster is a cloud to everyone; a caster also perceives living targets as clouds. */
    fun renderAsVeilCloud(entity: net.minecraft.world.entity.LivingEntity?, viewer: net.minecraft.world.entity.Entity?): Boolean {
        return entity != null && (isVeilStep(entity) || isVeilStep(viewer))
    }

    fun suppressTraces(entity: net.minecraft.world.entity.Entity?): kotlin.Boolean {
        return VeilbornStealthCompat.isAuroralMirage(entity) || VeilbornStealthCompat.isVeilStep(entity)
    }

    private fun hasActiveMarker(entity: net.minecraft.world.entity.Entity?, power: net.minecraft.resources.ResourceLocation?): kotlin.Boolean {
        if (entity == null) return false
        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(entity)
        return container != null && container.hasPower(power) && !container.isSuppressed(power)
    }

    private fun id(path: kotlin.String): net.minecraft.resources.ResourceLocation {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.CONTENT_NAMESPACE, path)
    }
}
