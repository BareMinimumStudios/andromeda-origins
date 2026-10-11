package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import andromeda.origins.compat.VeilStepManager
import andromeda.origins.platform.DamageCallback


 /** Ten-percent combat evasion passive for standard and Champion Veilborn.  */
object VeilbornShimmer {
    const val EVADE_CHANCE: kotlin.Float = 0.10f
    val SHIMMER: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
    AndromedaOrigins.CONTENT_NAMESPACE,
    "veilborn/shimmer"
    )

    fun register() {
        andromeda.origins.platform.Platform.damage(DamageCallback callback@ { entity: net.minecraft.world.entity.LivingEntity, source: net.minecraft.world.damagesource.DamageSource, amount: kotlin.Float ->
            // Veil Step exists outside normal combat: it neither deals nor receives ordinary damage.
        val attacker: net.minecraft.world.entity.Entity? = source.getEntity()
        val attackingPlayer = attacker as? net.minecraft.server.level.ServerPlayer
        if (attackingPlayer != null && VeilStepManager.isActive(attacker)) {
            return@callback false
        }
        val targetPlayer = entity as? net.minecraft.server.level.ServerPlayer
        if (targetPlayer != null && VeilStepManager.isActive(entity)) {
            return@callback false
        }

        val player = entity as? net.minecraft.server.level.ServerPlayer
        if ((player == null) || attacker == null || attacker === entity) {
            return@callback true
        }

        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(entity)
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
