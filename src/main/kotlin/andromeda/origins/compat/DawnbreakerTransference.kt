package andromeda.origins.compat

import dev.overgrown.apoli.PowerContainerAttachment
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import dev.overgrown.apoli.power.builtin.AttributePower
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import java.util.UUID

/** Per-caster leases, independent of Gorgon helpers. Disable one caster without erasing another's contribution. */
object DawnbreakerTransference {
    private fun id(s: String) = ResourceLocation.parse("andromeda_origins:$s")
    private val source = id("dawnbreaker_effect")
    private val power = id("dawnbreaker/secondary")
    private val boost = id("dawnbreaker/helper/transference_boost")
    private val weak = id("dawnbreaker/helper/transferred")
    private val boosts = mutableMapOf<UUID, Int>()
    private val victims = mutableMapOf<LivingEntity, MutableMap<UUID, Int>>()
    private fun lease(e: LivingEntity, root: ResourceLocation, n: Int) {
        PowerContainer.of(e)?.let { val key = id(root.path + "_lease"); if (PowerResources.read(it, key).orElse(-1.0) != n.toDouble()) { PowerResources.write(it, key, n.toDouble()); it.markDirty() } }
    }
    fun apply(caster: ServerPlayer, target: LivingEntity) {
        val now = caster.server.tickCount
        PowerContainerAttachment.getOrCreate(caster)?.let { it.addPower(boost, source); it.markDirty() }
        boosts[caster.uuid] = now + 300; lease(caster, boost, 300)
        val owners = victims.getOrPut(target) { mutableMapOf() }
        owners[caster.uuid] = ((owners[caster.uuid] ?: now).coerceAtLeast(now) + 300).coerceAtMost(now + 1200)
        PowerContainerAttachment.getOrCreate(target)?.let { it.addPower(weak, source); it.markDirty() }
        lease(target, weak, owners.values.max() - now)
    }
    private fun remove(e: LivingEntity, root: ResourceLocation) {
        PowerContainer.of(e)?.let { c -> if (c.removePower(root, source)) c.markDirty(); if (root == boost && !c.hasPower(id(root.path + "_speed"))) AttributePower.purge(e, id(root.path + "_speed")) }
    }
    fun clear(caster: ServerPlayer) {
        if (boosts.remove(caster.uuid) != null) PowerContainer.of(caster)?.let { c ->
            PowerResources.write(c, id("common/figura_2"), 0.0); c.markDirty()
        }
        remove(caster, boost)
        for ((target, owners) in victims.toMap()) { owners.remove(caster.uuid); if (owners.isEmpty()) { remove(target, weak); victims.remove(target) } }
        // Persisted leases with no live owner are stale; retain contributions from other live casters.
        if (caster !in victims) remove(caster, weak)
    }
    fun tick(server: MinecraftServer) {
        for ((uuid, until) in boosts.toMap()) {
            val player = server.playerList.getPlayer(uuid)
            if (player == null || server.tickCount >= until || !Dawnbreaker.usable(player, power)) {
                boosts.remove(uuid); player?.let {
                    remove(it, boost)
                    if (!Dawnbreaker.usable(it, power)) PowerContainer.of(it)?.let { c ->
                        PowerResources.write(c, id("common/figura_2"), 0.0)
                        c.removePower(id("gorgon/helper/intimidating"), ResourceLocation.parse("minecraft:state")); c.markDirty()
                    }
                }
            } else lease(player, boost, until - server.tickCount)
        }
        for ((target, owners) in victims.toMap()) {
            owners.entries.removeIf { (owner, until) -> server.tickCount >= until || server.playerList.getPlayer(owner)?.let { Dawnbreaker.usable(it, power) && Dawnbreaker.validTarget(it, target) } != true }
            if (!target.isAlive || target.isRemoved || owners.isEmpty()) { remove(target, weak); victims.remove(target) }
            else lease(target, weak, owners.values.max() - server.tickCount)
        }
    }
    fun stop(server: MinecraftServer) {
        for (uuid in boosts.keys) server.playerList.getPlayer(uuid)?.let { remove(it, boost) }
        for (target in victims.keys) remove(target, weak)
        boosts.clear(); victims.clear()
    }
}
