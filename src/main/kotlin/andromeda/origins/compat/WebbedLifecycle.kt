package andromeda.origins.compat

import andromeda.origins.platform.Platform
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.builtin.ResourcePower
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/** Webbing is a short combat effect, never a persistent login or respawn restriction. */
object WebbedLifecycle {
    private val webbed = id("arachne/helper/webbed")
    private val remaining = id("arachne/helper/webbed_remove")
    private val restrained = id("common/restrained")
    private val control = ResourceLocation.parse("minecraft:ccontrol")
    private val otherRestraints = listOf(id("common/petrified"), id("gorgon/helper/constricted"))

    fun register() {
        Platform.join { handler, _, _ -> clear(handler.player) }
        Platform.respawn { old, player, _ -> clear(old); clear(player) }
        Platform.death { entity, _, _ ->
            if (entity is ServerPlayer) clear(entity)
            true
        }
        Platform.serverTick { server -> server.playerList.players.forEach(::tick) }
    }

    fun tick(player: ServerPlayer) {
        val container = PowerContainer.of(player) ?: return
        if (container.hasPower(webbed) && ResourcePower.readValue(container, remaining).orElse(0) <= 0) {
            clear(player)
        } else if (player.tickCount % 20 == 0) {
            // Expiry above remains immediate. Re-scan legacy orphan grants once per second,
            // rather than copying every player's whole power set on every normal tick.
            reconcile(player, container)
        }
    }

    fun clear(player: ServerPlayer) {
        val container = PowerContainer.of(player) ?: return
        removeTree(container, webbed)
        reconcile(player, container)
        container.markDirty()
    }

    private fun reconcile(player: ServerPlayer, container: PowerContainer) {
        if (!container.hasPower(webbed) && otherRestraints.none(container::hasPower)) {
            removeTree(container, restrained)
        }
        RestraintCleanup.clean(player)
    }

    private fun removeTree(container: PowerContainer, root: ResourceLocation) {
        for (power in container.allPowers().toList()) {
            if (power == root || (power.namespace == root.namespace && power.path.startsWith(root.path + "_"))) {
                container.removePower(power, control)
            }
        }
    }

    private fun id(path: String) = ResourceLocation.parse("andromeda_origins:$path")
}
