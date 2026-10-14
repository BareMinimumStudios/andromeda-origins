package andromeda.origins.compat

import andromeda.origins.platform.Platform
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.builtin.AttributePower
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.Attributes

/** Clear a revoked restraint's speed modifier without changing health or other modifiers. */
object RestraintCleanup {
    private val power = ResourceLocation.parse("andromeda_origins:common/restrained_speed")
    private val modifier = ResourceLocation.parse("apoli:attribute/andromeda_origins/common_restrained_speed/0")

    fun register() {
        Platform.serverTick { server -> server.playerList.players.forEach(::clean) }
    }

    fun clean(player: ServerPlayer) {
        val speed = player.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
        if (!speed.hasModifier(modifier)) return
        val container = PowerContainer.of(player)
        if (container != null && container.hasPower(power) && !container.isSuppressed(power)) return
        AttributePower.purge(player, power)
        speed.removeModifier(modifier)
    }
}
