package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins


 /** Direct indexed Opoli lookup used by Undetectable AI/render compatibility.  */
object UndetectableCompat {
    val UNDETECTABLE: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
    AndromedaOrigins.CONTENT_NAMESPACE,
    "common/undetectable"
    )

    fun isUndetectable(entity: net.minecraft.world.entity.Entity?): kotlin.Boolean {
        if (entity == null) return false
        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(entity)
        return container != null && container.hasPower(UndetectableCompat.UNDETECTABLE)
        && !container.isSuppressed(UndetectableCompat.UNDETECTABLE)
    }
}
