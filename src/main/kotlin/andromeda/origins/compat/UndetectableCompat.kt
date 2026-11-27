package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import dev.overgrown.apoli.power.PowerContainer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity


 /** Direct indexed Opoli lookup used by Undetectable AI/render compatibility.  */
object UndetectableCompat {
    val UNDETECTABLE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
    AndromedaOrigins.CONTENT_NAMESPACE,
    "common/undetectable"
    )

    fun isUndetectable(entity: Entity?): Boolean {
        if (entity == null) return false
        val container: PowerContainer? = PowerContainer.of(entity)
        return container != null && container.hasPower(UndetectableCompat.UNDETECTABLE)
        && !container.isSuppressed(UndetectableCompat.UNDETECTABLE)
    }
}
