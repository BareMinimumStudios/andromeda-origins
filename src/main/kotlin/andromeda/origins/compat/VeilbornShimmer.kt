package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import andromeda.origins.compat.VeilStepManager
import andromeda.origins.platform.DamageCallback
import andromeda.origins.platform.Platform
import dev.overgrown.apoli.power.PowerContainer
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity


 /** Ten-percent combat evasion passive for standard and Champion Veilborn.  */
object VeilbornShimmer {
    const val EVADE_CHANCE: Float = 0.10f
    val SHIMMER: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
    AndromedaOrigins.CONTENT_NAMESPACE,
    "veilborn/shimmer"
    )

    fun register() {
        Platform.damage(DamageCallback callback@ { entity: LivingEntity, source: DamageSource, amount: Float ->
            // Veil Step exists outside normal combat: it neither deals nor receives ordinary damage.
        val attacker: Entity? = source.getEntity()
        val attackingPlayer = attacker as? ServerPlayer
        if (attackingPlayer != null && VeilStepManager.isActive(attacker)) {
            return@callback false
        }
        val targetPlayer = entity as? ServerPlayer
        if (targetPlayer != null && VeilStepManager.isActive(entity)) {
            return@callback false
        }

        val player = entity as? ServerPlayer
        if ((player == null) || attacker == null || attacker === entity) {
            return@callback true
        }

        val container: PowerContainer? = PowerContainer.of(entity)
        if (container == null || !container.hasPower(VeilbornShimmer.SHIMMER) || container.isSuppressed(VeilbornShimmer.SHIMMER)) {
            return@callback true
        }

        if (entity.getRandom().nextFloat() >= VeilbornShimmer.EVADE_CHANCE) {
            return@callback true
        }

        VeilStepManager.playShimmer(entity)
        false
        })
    }
}
