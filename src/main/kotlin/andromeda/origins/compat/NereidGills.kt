package andromeda.origins.compat

import dev.overgrown.apoli.power.ApoliPowers
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.builtin.ResourcePower
import dev.overgrown.apoli.condition.context.EntityCtx
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffects

/** Keep gill conditions/configuration native; enforce dry air after the player update. */
object NereidGills {
    private val power = ResourceLocation.parse("andromeda_origins:nereid/gills")

    private val wet = ResourceLocation.parse("andromeda_origins:common/wet")
    private val debuff = ResourceLocation.parse("minecraft:debuff")
    private val hydration = listOf("nereid/helper/nereidwet", "selkie/helper/selkiewet").map {
        ResourceLocation.parse("andromeda_origins:$it")
    }

    /** A leftover shared Wet grant must not outlive the actual hydration helpers. */
    fun clearOrphanWet(player: ServerPlayer) {
        val container = PowerContainer.of(player) ?: return
        if (!container.hasPower(power) || !container.sourcesOf(wet).contains(debuff)) return
        if (hydration.any { helper ->
            container.hasPower(helper) && !container.isSuppressed(helper) &&
                ResourcePower.readValue(container, ResourceLocation.fromNamespaceAndPath(
                    helper.namespace, helper.path + "_remove")).orElse(0) > 0
        }) return
        // Only the source owned by our hydration callbacks; preserve unrelated grants.
        for (id in container.allPowers().toList()) {
            if (id == wet || (id.namespace == wet.namespace && id.path.startsWith(wet.path + "_"))) {
                container.removePower(id, debuff)
            }
        }
        container.markDirty()
    }

    fun isDry(player: ServerPlayer): Boolean {
        clearOrphanWet(player)
        val container = PowerContainer.of(player) ?: return false
        if (!container.hasPower(power) || container.isSuppressed(power)) return false
        // Hybrid Aquatic diving helmets grant native Water Breathing; recheck every tick,
        // so helmet removal/effect expiry cannot leave a permanent breathing exemption.
        if (player.hasEffect(MobEffects.WATER_BREATHING) || player.hasEffect(MobEffects.CONDUIT_POWER)) return false
        val definition = ApoliPowers.get(power) ?: return false
        return definition.condition().map { it.test(EntityCtx.of(player, player.level())) }.orElse(true)
    }
}
