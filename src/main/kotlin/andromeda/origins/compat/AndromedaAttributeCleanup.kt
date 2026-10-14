package andromeda.origins.compat

import dev.overgrown.apoli.power.ApoliPowers
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.builtin.AttributePower
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.registries.BuiltInRegistries

/** Upstream removal callbacks run before the last grant disappears from the container. */
object AndromedaAttributeCleanup {
    fun afterRemoval(container: PowerContainer, power: ResourceLocation) {
        if (power.namespace != "andromeda_origins" || container.hasPower(power)) return
        val owner = container.owner() ?: return
        val definition = ApoliPowers.get(power) ?: return
        if (owner.level().isClientSide || definition.type() !is AttributePower) return
        val health = owner.health
        AttributePower.purge(owner, power)
        // Saved modifiers may exist before upstream's runtime cache has been rebuilt.
        val cfg = definition.config() as AttributePower.Cfg
        cfg.modifiers().indices.forEach { index ->
            val modifier = ResourceLocation.fromNamespaceAndPath("apoli",
                "attribute/${power.namespace}/${power.path.replace('/', '_')}/$index")
            for (holder in BuiltInRegistries.ATTRIBUTE.asHolderIdMap()) {
                owner.getAttribute(holder)?.removeModifier(modifier)
            }
        }
        // Losing an attribute must never heal or revive its owner.
        if (health.isFinite() && owner.maxHealth.isFinite()) owner.health = health.coerceAtMost(owner.maxHealth)
    }
}
