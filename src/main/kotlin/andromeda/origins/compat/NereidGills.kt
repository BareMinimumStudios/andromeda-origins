package andromeda.origins.compat

import dev.overgrown.apoli.power.ApoliPowers
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.condition.context.EntityCtx
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffects

/** Keep gill conditions/configuration native; enforce dry air after the player update. */
object NereidGills {
    private val power = ResourceLocation.parse("andromeda_origins:nereid/gills")

    fun isDry(player: ServerPlayer): Boolean {
        val container = PowerContainer.of(player) ?: return false
        if (!container.hasPower(power) || container.isSuppressed(power)) return false
        // Hybrid Aquatic diving helmets grant native Water Breathing; recheck every tick,
        // so helmet removal/effect expiry cannot leave a permanent breathing exemption.
        if (player.hasEffect(MobEffects.WATER_BREATHING) || player.hasEffect(MobEffects.CONDUIT_POWER)) return false
        val definition = ApoliPowers.get(power) ?: return false
        return definition.condition().map { it.test(EntityCtx.of(player, player.level())) }.orElse(true)
    }
}
