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
    private data class Held(val projectile: Projectile, val velocity: Vec3, val owner: Entity?, val from: Vec3)
    private data class Guard(val started: Int, var refreshed: Int, val held: MutableList<Held> = mutableListOf())
    private data class Release(val caster: UUID, val held: Held, val level: net.minecraft.server.level.ServerLevel, val at: Int, val aim: Vec3)
    private val releasing = mutableListOf<Release>()
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
        Dawnbreaker.success(player, "Kneel.")
        if (Dawnbreaker.radiant(player)) player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, player.x, player.y + .12, player.z, 6, .12, .01, .12, .015)
    }
    fun hold(player: ServerPlayer) {
        if (!Dawnbreaker.usable(player, guard)) return
        val now = player.server.tickCount
        guards.getOrPut(player.uuid) {
            Dawnbreaker.abilitySound(player, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, .2f, 1.6f)
            Guard(now, now)
        }.refreshed = now
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
    private fun restore(held: Held) {
        val p = held.projectile
        frozen.remove(p)
        if (p.removeTag(RECOVER_GRAVITY)) p.setNoGravity(false)
        if (!p.isRemoved) { p.owner = held.owner; p.deltaMovement = held.velocity; p.hurtMarked = true }
    }
    private fun aim(player: ServerPlayer): Vec3 {
        val eye = player.eyePosition; val end = eye.add(player.lookAngle.scale(48.0))
        val wall = player.serverLevel().clip(ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
        return player.serverLevel().getEntitiesOfClass(LivingEntity::class.java, player.boundingBox.expandTowards(player.lookAngle.scale(48.0)).inflate(1.0)) { Dawnbreaker.validTarget(player, it) }
            .mapNotNull { target -> target.boundingBox.inflate(.08).clip(eye, end).orElse(null) }
            .filter { it.distanceToSqr(eye) < wall.location.distanceToSqr(eye) }
            .minByOrNull { it.distanceToSqr(eye) } ?: wall.location
    }
    private fun orient(p: Projectile, at: Vec3) {
        val d = at.subtract(p.position())
        if (d.lengthSqr() < .00001) return
        p.yRot = Math.toDegrees(atan2(d.x, d.z)).toFloat()
        p.xRot = Math.toDegrees(atan2(d.y, sqrt(d.x * d.x + d.z * d.z))).toFloat()
        p.hurtMarked = true
    }
    private fun finish(player: ServerPlayer?, state: Guard, fire: Boolean) {
        if (fire && player != null) {
            if (state.held.isNotEmpty()) {
                DawnbreakerFx.cue(player, 2)
                Dawnbreaker.abilitySound(player, net.minecraft.sounds.SoundEvents.TRIDENT_THROW.value(), .5f, 1.4f)
            }
            val target = if (state.held.isNotEmpty()) aim(player) else player.eyePosition
            state.held.forEach { held -> releasing.add(Release(player.uuid, held, player.serverLevel(), player.server.tickCount, target)) }
        } else state.held.forEach(::restore)
        if (player != null) { write(player, "stillwater_active", 0.0); write(player, "stillwater_count", 0.0); write(player, "stillwater_radiance", 0.0) }
    }
    private fun tickRelease(server: MinecraftServer) {
        for (release in releasing.toList()) {
            val player = server.playerList.getPlayer(release.caster); val p = release.held.projectile
            if (player == null || !Dawnbreaker.usable(player, guard) || player.level() !== release.level || p.level() !== release.level || p.isRemoved) {
                restore(release.held); releasing.remove(release); continue
            }
            orient(p, release.aim)
            if (server.tickCount < release.at) continue
            releasing.remove(release); frozen.remove(p)
            if (p.removeTag(RECOVER_GRAVITY)) p.setNoGravity(false)
            // Fire from the frozen interception point; fall back to the muzzle if a block moved into it.
            if (!release.level.noCollision(p, p.boundingBox)) p.setPos(player.eyePosition.add(player.lookAngle.scale(.9)))
            p.owner = player; p.addTag("andromeda_stillwater_returned")
            val d = release.aim.subtract(p.position())
            val previousSpeed = release.held.velocity.length().coerceIn(1.0, 3.0)
            val returnSpeed = (previousSpeed * 3.0).coerceIn(6.0, 8.0)
            // Arrow damage multiplies base damage by speed: acceleration must not silently multiply damage.
            if (p is net.minecraft.world.entity.projectile.AbstractArrow) p.setBaseDamage(p.baseDamage * previousSpeed / returnSpeed)
            p.shoot(d.x, d.y, d.z, returnSpeed.toFloat(), 0f)
            p.hurtMarked = true
        }
    }
    private fun releaseGround(target: LivingEntity) {
        PowerContainer.of(target)?.let { c -> c.removePower(helper, source); c.markDirty() }
        if (PowerContainer.of(target)?.hasPower(id("helper/kneeling_speed")) != true) AttributePower.purge(target, id("helper/kneeling_speed"))
        grounded.remove(target)
    }
    fun clear(player: ServerPlayer) {
        guards.remove(player.uuid)?.let { finish(player, it, false) }
        for (release in releasing.filter { it.caster == player.uuid }) { restore(release.held); releasing.remove(release) }
        releaseGround(player)
        for ((target, state) in grounded.toMap()) { state.owners.remove(player.uuid); if (state.owners.isEmpty()) releaseGround(target) }
        write(player, "stillwater_active", 0.0); write(player, "stillwater_count", 0.0)
    }
    fun stop() { for (state in guards.values) finish(null, state, false); releasing.forEach { restore(it.held) }; releasing.clear(); guards.clear(); frozen.clear(); for (e in grounded.keys.toList()) releaseGround(e) }
    fun tick(server: MinecraftServer) {
        tickRelease(server)
        for ((target, state) in grounded.toMap()) {
            state.owners.entries.removeIf { (owner, until) -> server.tickCount >= until || server.playerList.getPlayer(owner)?.let { Dawnbreaker.usable(it, kneel) && Dawnbreaker.validTarget(it, target) } != true }
            if (state.owners.isEmpty() || !target.isAlive || target.isRemoved) { releaseGround(target); continue }
            write(target, "helper/kneeling_lease", (state.owners.values.max() - server.tickCount).toDouble())
            write(target, "helper/kneeling_radiance", if (state.owners.keys.any { server.playerList.getPlayer(it)?.let(Dawnbreaker::radiant) == true }) 1.0 else 0.0)
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
                if (state.held.size + releasing.count { it.caster == uuid } >= 8) break
                if (!player.hasLineOfSight(p)) continue
                if (state.held.isEmpty()) {
                    DawnbreakerFx.cue(player, 1)
                    Dawnbreaker.abilitySound(player, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, .35f, 1.4f)
                }
                state.held.add(Held(p, p.deltaMovement, p.owner, p.position())); frozen.add(p)
                if (!p.isNoGravity) { p.addTag(RECOVER_GRAVITY); p.setNoGravity(true) }
            }
            // Force-like stasis: do not reel shots into an orbit or turn them while held.
            for (held in state.held) {
                held.projectile.setPos(held.from)
                held.projectile.deltaMovement = Vec3.ZERO
                held.projectile.hurtMarked = true
            }
            write(player, "stillwater_active", (160 - (server.tickCount - state.started)).toDouble())
            write(player, "stillwater_count", state.held.size.toDouble())
            write(player, "stillwater_radiance", if (Dawnbreaker.radiant(player)) 1.0 else 0.0)
        }
    }
}
