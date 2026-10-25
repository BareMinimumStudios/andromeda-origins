package andromeda.origins.compat

import andromeda.origins.platform.Platform
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/** Rebuild shared restrictions from their actual owners, not persisted callback totals. */
object CrowdControlLifecycle {
    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath("andromeda_origins", path)
    private val source = ResourceLocation.fromNamespaceAndPath("minecraft", "ccontrol")
    private val petrified = id("common/petrified")
    private val infatuated = id("siren/helper/infatuated")
    private val webbed = id("arachne/helper/webbed")
    private val constricted = id("gorgon/helper/constricted")
    private val ophidianed = id("gorgon/helper/ophidianed")
    private val timed = listOf(ophidianed, infatuated, webbed, constricted)
    private data class Shared(val power: ResourceLocation, val owners: List<ResourceLocation>) {
        val counter = ResourceLocation.fromNamespaceAndPath(power.namespace, power.path + "_sources")
    }
    // Resolve the parent petrification first; it owns the remaining restrictions.
    private val shared = listOf(
        Shared(petrified, listOf(ophidianed)),
        Shared(id("common/restrained"), listOf(petrified, webbed, constricted)),
        Shared(id("common/silenced"), listOf(id("selkie/helper/groundweak"), petrified, infatuated)),
        Shared(id("common/pacified"), listOf(petrified, infatuated))
    )

    fun register() {
        Platform.serverTick { server ->
            if (server.tickCount % 20 == 0) server.playerList.players.forEach(::reconcile)
        }
        Platform.join { handler, _, _ -> clearGorgon(handler.player) }
        Platform.respawn { old, player, _ -> clearGorgon(old); clearGorgon(player) }
        Platform.death { entity, _, _ ->
            if (entity is ServerPlayer) clearGorgon(entity)
            true
        }
    }

    /** Combat petrification must not survive login or respawn. Keep unrelated grants. */
    private fun clearGorgon(player: ServerPlayer) {
        val container = PowerContainer.of(player) ?: return
        for (root in listOf(ophidianed, constricted, petrified)) removeControlTree(container, root)
        reconcile(player)
        container.markDirty()
    }

    /** Explicit operator repair clears temporary crowd control without healing or changing Origins. */
    fun clearTemporary(player: ServerPlayer) {
        val container = PowerContainer.of(player) ?: return
        for (root in timed + shared.map { it.power }) removeControlTree(container, root)
        reconcile(player)
        container.markDirty()
    }

    private fun removeControlTree(container: PowerContainer, root: ResourceLocation) {
        // Remove the normal root first so MultiplePower releases its native child ownership.
        container.removePower(root, source)
        for (power in container.allPowers().toList()) {
            if (power.namespace == root.namespace && power.path.startsWith(root.path + "_"))
                container.removePower(power, source)
        }
    }

    fun reconcile(player: ServerPlayer) {
        val container = PowerContainer.of(player) ?: return
        for (helper in timed) {
            val remaining = ResourceLocation.fromNamespaceAndPath(helper.namespace, helper.path + "_remove")
            if (source in container.sourcesOf(helper) &&
                (!container.hasPower(remaining) || PowerResources.read(container, remaining).asInt <= 0))
                container.removePower(helper, source)
        }
        for (effect in shared) {
            val active = effect.owners.count { container.hasPower(it) && !container.isSuppressed(it) }
            if (active > 0) {
                if (source !in container.sourcesOf(effect.power)) container.addPower(effect.power, source)
                if (PowerResources.read(container, effect.counter).asInt != active)
                    PowerResources.write(container, effect.counter, active)
            } else if (source in container.sourcesOf(effect.power)) {
                // Preserve grants belonging to anyone else, including another mod or command source.
                container.removePower(effect.power, source)
            }
        }
        NereidGills.clearOrphanWet(player)
        RestraintCleanup.clean(player)
    }
}
