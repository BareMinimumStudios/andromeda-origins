package andromeda.origins.compat

import andromeda.origins.platform.Platform
import dev.overgrown.apoli.condition.context.EntityCtx
import dev.overgrown.apoli.power.ApoliPowers
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.builtin.AttributePower
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.Attributes

/** Remove only the sunlight modifier left behind by transient helper revocation. */
object LichlingSunlight {
    private val power = ResourceLocation.parse("andromeda_origins:lichling/helper/sunweak_speed")
    private val modifiers = listOf(
        ResourceLocation.parse("apoli:attribute/andromeda_origins/lichling_helper_sunweak_speed/0"),
        ResourceLocation.parse("apoli:attribute/andromeda_origins/lichling/helper/sunweak_speed/0"),
        ResourceLocation.parse("andromeda_origins:lichling_helper_sunweak_speed")
    )

    fun register() {
        Platform.serverTick { server -> server.playerList.players.forEach(::clean) }
    }

    fun clean(player: ServerPlayer) {
        val speed = player.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
        if (modifiers.none { speed.hasModifier(it) }) return
        val container = PowerContainer.of(player)
        val definition = ApoliPowers.get(power)
        val active = container != null && container.hasPower(power) && !container.isSuppressed(power) &&
            definition != null && definition.condition().map {
                it.test(EntityCtx.of(player, player.level()))
            }.orElse(false)
        if (active) return
        // Purge the upstream cache as well, allowing the next sun exposure to apply normally.
        AttributePower.purge(player, power)
        modifiers.forEach { speed.removeModifier(it) }
    }
}
