package andromeda.origins.compat

import dev.overgrown.apoli.PowerContainerAttachment
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import dev.overgrown.apoli.power.builtin.AttributePower
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.ClipContext
import java.util.UUID
import kotlin.math.*

/** Bounded, server-owned temporary state. Never serializes a frozen projectile or edits player abilities. */
object SovereignArts {
    private fun id(s: String) = ResourceLocation.parse("andromeda_origins:dawnbreaker/$s")
    private val kneel = id("kneel")
    private val guard = id("stillwater")
    private val helper = id("helper/kneeling")
    private val source = ResourceLocation.parse("andromeda_origins:dawnbreaker_effect")
    private data class Grounded(val owners: MutableMap<UUID, Int>)
    private val grounded = mutableMapOf<LivingEntity, Grounded>()
    private data class Held(val projectile: Projectile, val velocity: Vec3, val owner: Entity?)
    private data class Guard(val started: Int, var refreshed: Int, val held: MutableList<Held> = mutableListOf())
    private val guards = mutableMapOf<UUID, Guard>()
    private val frozen = mutableSetOf<Projectile>()
    private fun read(e: LivingEntity, path: String) = PowerContainer.of(e)?.let { PowerResources.read(it, id(path)).orElse(0.0) } ?: 0.0
    private fun write(e: LivingEntity, path: String, n: Double) {
        PowerContainer.of(e)?.let { if (PowerResources.read(it, id(path)).orElse(n) != n) { PowerResources.write(it, id(path), n); it.markDirty() } }
    }
    fun kneel(player: ServerPlayer) {
        if (!Dawnbreaker.usable(player, kneel)) return
        val targets = player.serverLevel().getEntitiesOfClass(LivingEntity::class.java, player.boundingBox.inflate(12.0)) {
            Dawnbreaker.validTarget(player, it) && it.distanceToSqr(player) <= 144 && player.hasLineOfSight(it) &&
                !(it is ServerPlayer && it.isCreative)
        }.sortedBy { it.distanceToSqr(player) }.take(32)
        if (targets.isEmpty()) { player.displayClientMessage(net.minecraft.network.chat.Component.literal("Kneel — no eligible enemies within 12 blocks."), true); return }
        for (target in targets) {
            val c = PowerContainerAttachment.getOrCreate(target) ?: continue
            c.addPower(helper, source); c.markDirty()
            grounded.getOrPut(target) { Grounded(mutableMapOf()) }.owners[player.uuid] = player.server.tickCount + 60
            write(target, "helper/kneeling_lease", 60.0)
            write(target, "helper/kneeling_radiance", if (Dawnbreaker.radiant(player)) 1.0 else 0.0)
            target.fallDistance = 0f
            if (target is ServerPlayer && target.isFallFlying) target.stopFallFlying()
        }
        write(player, "kneel_cooldown", 0.0)
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Kneel."), true)
        if (Dawnbreaker.radiant(player)) player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, player.x, player.y + .12, player.z, 6, .12, .01, .12, .015)
    }
    fun hold(player: ServerPlayer) {
        if (!Dawnbreaker.usable(player, guard)) return
        val now = player.server.tickCount
        guards.getOrPut(player.uuid) { Guard(now, now) }.refreshed = now
    }
    private const val RECOVER_GRAVITY = "andromeda_stillwater_restore_gravity"
    fun isFrozen(projectile: Projectile): Boolean {
        if (projectile.level().isClientSide) return false
        // A chunk save/crash must not leave a captured projectile hovering after runtime state is gone.
        if (projectile !in frozen && projectile.removeTag(RECOVER_GRAVITY)) projectile.setNoGravity(false)
        return projectile in frozen
    }
    private fun eligible(p: Projectile, actor: ServerPlayer): Boolean {
        if (p.isNoGravity || p.isRemoved || p.owner === actor || p in frozen || "andromeda_dawn_light" in p.tags) return false
        if (p.type !in listOf(EntityType.ARROW, EntityType.SPECTRAL_ARROW, EntityType.SNOWBALL, EntityType.EGG)) return false
        val owner = p.owner as? LivingEntity ?: return false
        if (!Dawnbreaker.validTarget(actor, owner)) return false
        val v = p.deltaMovement
        return v.lengthSqr() > .01 && v.dot(actor.eyePosition.subtract(p.position())) > 0
    }
    private fun slot(player: ServerPlayer, i: Int): Vec3 {
        val local = DawnbreakerLayout.guardSlot(i)
        val yaw = Math.toRadians(player.yRot.toDouble())
        val right = Vec3(cos(yaw), 0.0, sin(yaw))
        val back = Vec3(sin(yaw), 0.0, -cos(yaw))
        return player.position().add(right.scale(local.x)).add(back.scale(-local.z)).add(0.0, local.y, 0.0)
    }
    private fun finish(player: ServerPlayer?, state: Guard, fire: Boolean) {
        var aim: Vec3? = null
        if (fire && player != null) {
            val eye = player.eyePosition
            val hit = player.serverLevel().clip(ClipContext(eye, eye.add(player.lookAngle.scale(48.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
            aim = hit.location
        }
        for (held in state.held) {
            val p = held.projectile
            frozen.remove(p)
            if (p.removeTag(RECOVER_GRAVITY)) p.setNoGravity(false)
            if (p.isRemoved) continue
            if (fire && player != null && p.level() === player.level()) {
                // Start above the shoulder, ahead of the caster, with ordinary vanilla collision/PvP rules.
                val at = player.eyePosition.add(player.lookAngle.scale(1.2))
                p.setPos(at); p.owner = player; p.addTag("andromeda_stillwater_returned")
                p.shoot(aim!!.x - at.x, aim.y - at.y, aim.z - at.z, held.velocity.length().coerceIn(1.0, 3.0).toFloat(), 0f)
            } else { p.owner = held.owner; p.deltaMovement = held.velocity }
            p.hurtMarked = true
        }
        if (player != null) { write(player, "stillwater_active", 0.0); write(player, "stillwater_count", 0.0); write(player, "stillwater_radiance", 0.0) }
    }
    private fun releaseGround(target: LivingEntity) {
        PowerContainer.of(target)?.let { c -> c.removePower(helper, source); c.markDirty() }
        if (PowerContainer.of(target)?.hasPower(id("helper/kneeling_speed")) != true) AttributePower.purge(target, id("helper/kneeling_speed"))
        grounded.remove(target)
    }
    fun clear(player: ServerPlayer) {
        guards.remove(player.uuid)?.let { finish(player, it, false) }
        releaseGround(player)
        for ((target, state) in grounded.toMap()) { state.owners.remove(player.uuid); if (state.owners.isEmpty()) releaseGround(target) }
        write(player, "stillwater_active", 0.0); write(player, "stillwater_count", 0.0)
    }
    fun stop() { for (state in guards.values) finish(null, state, false); guards.clear(); frozen.clear(); for (e in grounded.keys.toList()) releaseGround(e) }
    fun tick(server: MinecraftServer) {
        for ((target, state) in grounded.toMap()) {
            state.owners.entries.removeIf { (owner, until) -> server.tickCount >= until || server.playerList.getPlayer(owner)?.let { Dawnbreaker.usable(it, kneel) && Dawnbreaker.validTarget(it, target) } != true }
            if (state.owners.isEmpty() || !target.isAlive || target.isRemoved) { releaseGround(target); continue }
            write(target, "helper/kneeling_lease", (state.owners.values.max() - server.tickCount).toDouble())
            target.fallDistance = 0f
            if (!target.onGround()) { target.deltaMovement = Vec3(target.deltaMovement.x * .5, -.35, target.deltaMovement.z * .5); target.hurtMarked = true }
            if (target is ServerPlayer && target.isFallFlying) target.stopFallFlying()
        }
        for ((uuid, state) in guards.toMap()) {
            val player = server.playerList.getPlayer(uuid)
            val usable = player != null && Dawnbreaker.usable(player, guard)
            if (!usable || server.tickCount - state.refreshed > 3 || server.tickCount - state.started >= 160) {
                guards.remove(uuid); finish(player, state, usable && server.tickCount - state.started < 160); continue
            }
            player!!
            // Dimension changes cancel instead of dragging projectiles between worlds.
            if (state.held.any { it.projectile.level() !== player.level() }) { guards.remove(uuid); finish(player, state, false); continue }
            state.held.removeIf { if (it.projectile.isRemoved) { frozen.remove(it.projectile); true } else false }
            for (p in player.serverLevel().getEntitiesOfClass(Projectile::class.java, player.boundingBox.inflate(6.0)) { eligible(it, player) }.sortedBy { it.distanceToSqr(player) }) {
                if (state.held.size >= 8) break
                if (!player.hasLineOfSight(p)) continue
                state.held.add(Held(p, p.deltaMovement, p.owner)); frozen.add(p)
                if (!p.isNoGravity) { p.addTag(RECOVER_GRAVITY); p.setNoGravity(true) }
            }
            state.held.forEachIndexed { i, held -> held.projectile.setPos(slot(player, i)); held.projectile.deltaMovement = Vec3.ZERO; held.projectile.hurtMarked = true }
            write(player, "stillwater_active", (160 - (server.tickCount - state.started)).toDouble())
            write(player, "stillwater_count", state.held.size.toDouble())
            write(player, "stillwater_radiance", if (Dawnbreaker.radiant(player)) 1.0 else 0.0)
        }
    }
}
