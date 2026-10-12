package andromeda.origins.compat

import andromeda.origins.config.AndromedaConfigApplier
import andromeda.origins.platform.Platform
import dev.overgrown.apoli.PowerContainerAttachment
import dev.overgrown.apoli.power.ApoliPowers
import dev.overgrown.apoli.power.builtin.AttributePower
import dev.overgrown.apoli.power.builtin.MultiplePower
import dev.overgrown.origins.component.PlayerOriginsAttachment
import dev.overgrown.origins.origin.OriginRegistry
import dev.overgrown.origins.OriginsWorldConfig
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.attributes.DefaultAttributes
import java.util.UUID

/** Rebuild only Origin-owned modifiers. Never clear the container or revive/heal a player. */
object PlayerOriginRepair {
    private val pending = mutableMapOf<UUID, Int>()
    private val managed = setOf(Attributes.MAX_HEALTH, Attributes.MOVEMENT_SPEED, Attributes.SCALE,
        Attributes.STEP_HEIGHT, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
        Attributes.KNOCKBACK_RESISTANCE, Attributes.ATTACK_SPEED)

    fun register() {
        Platform.join { handler, _, _ -> pending[handler.player.uuid] = 20 }
        Platform.respawn { _, player, _ -> pending[player.uuid] = 20 }
        Platform.disconnect { handler, _ -> pending.remove(handler.player.uuid) }
        Platform.serverStopped { pending.clear() }
        Platform.serverTick { server ->
            for ((id, delay) in pending.toMap()) {
                if (delay > 0) { pending[id] = delay - 1; continue }
                val player = server.playerList.getPlayer(id)
                pending.remove(id)
                if (player != null) repair(player, false)
            }
        }
        Platform.commands { dispatcher, _, _ ->
            for (name in listOf("repair", "repair_attributes")) {
                dispatcher.register(Commands.literal("andromedaorigins").then(
                    Commands.literal(name).requires { it.hasPermission(2) }
                        .then(Commands.argument("player", EntityArgument.player()).executes { context ->
                            val player = EntityArgument.getPlayer(context, "player")
                            val result = repair(player, true)
                            if (result == null) {
                                context.source.sendFailure(Component.literal("Repair skipped: current Origin selection is unavailable."))
                                0
                            } else {
                                context.source.sendSuccess({ Component.literal("Repaired ${player.gameProfile.name}: $result modifiers removed; current Origin attributes rebuilt. No healing performed.") }, true)
                                1
                            }
                        })))
            }
        }
    }

    /** Manual repair also restores the eight vanilla bases the legacy repair managed. */
    fun repair(player: ServerPlayer, resetBases: Boolean): Int? {
        val state = PlayerOriginsAttachment.get(player) ?: return null
        val selected = (state.snapshot().values + state.swapSnapshot().values).toSet()
        if (selected.isEmpty()) return null
        val container = PowerContainerAttachment.get(player) ?: return null
        val medievalActive = selected.any { it.namespace == "medievalorigins" }
        var pixie = container.allSources().any { it.toString() == "medievalorigins:pixie" } ||
            container.allPowers().any { it.namespace == "medievalorigins" && it.path.startsWith("pixie/") }
        val oldHealth = player.health
        var removed = 0
        try {
            val wanted = mutableSetOf<ResourceLocation>()
            fun collect(id: ResourceLocation) {
                if (!wanted.add(id)) return
                val power = ApoliPowers.get(id) ?: return
                if (power.type() is MultiplePower) {
                    for (child in (power.config() as MultiplePower.Cfg).subPowerIds()) collect(child)
                }
            }
            for (originId in selected) {
                OriginRegistry.get(originId)?.powersFor(player)?.forEach { collect(it) }
            }
            // Legacy Apoli used Origin IDs as sources; Overgrown uses layer IDs. Remove only
            // obsolete grants from old Andromeda selections, preserving shared current powers.
            for (source in container.allSources().toList()) {
                if (source.namespace != "andromeda_origins" || source in selected || OriginRegistry.get(source) == null) continue
                for (id in container.allPowers().toList()) {
                    if (id !in wanted && source in container.sourcesOf(id)) container.removePower(id, source)
                }
            }
            if (!medievalActive) {
                // Remove complete legacy sources, including vanilla dietary powers they granted.
                for (source in container.allSources().toList()) {
                    if (source.namespace == "medievalorigins") container.removeAllFromSource(source)
                }
                for (id in container.allPowers().toList()) {
                    if (id.namespace == "medievalorigins") container.removePowerCompletely(id)
                }
                player.removeTag("siren_seduce")
            }
            // Restore missing grants without removing/re-adding live powers or their stored state.
            for ((layer, originId) in state.snapshot()) {
                if (originId.namespace != "andromeda_origins" || OriginsWorldConfig.blocks(layer, originId)) continue
                val origin = OriginRegistry.get(originId) ?: continue
                val source = ResourceLocation.fromNamespaceAndPath(layer.namespace, "layer/${layer.path}")
                fun grant(id: ResourceLocation, visited: MutableSet<ResourceLocation>) {
                    if (!visited.add(id)) return
                    val power = ApoliPowers.get(id) ?: return
                    if (!container.hasPower(id)) container.addPower(id, source)
                    if (power.type() is MultiplePower) {
                        for (child in (power.config() as MultiplePower.Cfg).subPowerIds()) grant(child, visited)
                    }
                }
                for (id in origin.powersFor(player)) grant(id, mutableSetOf())
            }
            val attributes = container.allPowers().mapNotNull { id ->
                val power = ApoliPowers.get(id) ?: return@mapNotNull null
                if (id.namespace != "andromeda_origins" || power.type() !is AttributePower) null
                else Triple(id, power.type() as AttributePower, power.config() as AttributePower.Cfg)
            }
            // Clear upstream's per-power cache too, so conditional powers re-evaluate correctly.
            for ((id, _, _) in attributes) AttributePower.purge(player, id)
            val defaults = DefaultAttributes.getSupplier(EntityType.PLAYER)
            for (holder in BuiltInRegistries.ATTRIBUTE.asHolderIdMap()) {
                val instance = player.getAttribute(holder) ?: continue
                for (modifier in instance.modifiers.toList()) {
                    val id = modifier.id()
                    val medieval = id.namespace == "medievalorigins" ||
                        (id.namespace == "apoli" && id.path.startsWith("attribute/medievalorigins/"))
                    if (medieval && id.path.contains("pixie")) pixie = true
                    if (id.namespace == "andromeda_origins" ||
                        (id.namespace == "apoli" && id.path.startsWith("attribute/andromeda_origins/")) ||
                        (medieval && !medievalActive)) {
                        instance.removeModifier(id)
                        removed++
                    }
                }
                if (resetBases && holder in managed && defaults.hasAttribute(holder)) {
                    instance.baseValue = defaults.getBaseValue(holder)
                }
            }
            AndromedaConfigApplier.applyPowerSuppression(player)
            for ((id, type, cfg) in attributes) {
                if (!container.isSuppressed(id)) type.tick(id, cfg, container)
            }
            if (pixie && !medievalActive && Platform.isModLoaded("pehkui")) {
                val source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput()
                val commands = player.server.commands
                if (commands.dispatcher.root.getChild("scale") != null) {
                    commands.performPrefixedCommand(source, "scale reset pehkui:eye_height @s")
                }
            }
            container.markDirty()
            return removed
        } finally {
            // Opoli update_health can adjust HP during a rebuild; repair must never heal/revive.
            player.health = oldHealth.coerceAtMost(player.maxHealth)
        }
    }
}
