package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import andromeda.origins.compat.AndromedaOriginTags
import andromeda.origins.compat.EnhancedFxCompat
import andromeda.origins.platform.JoinCallback
import andromeda.origins.platform.Platform
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import java.lang.Float as JavaFloat
import java.lang.Integer as JavaInteger
import java.util.HashMap
import java.util.HashSet
import java.util.Set as JavaSet
import java.util.UUID
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Abilities
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3


 /**
 * Server-authoritative Veil Step state.
 *
 *
 * Traversal itself deliberately uses vanilla flight movement rather than server-side movement
 * corrections. This keeps steering smooth and immediate while preserving ordinary solid-block
 * collision. The server owns the 5-second lifetime, combat/stealth state, entity-crossing tears,
 * cooldown, visible shadow-cloud presentation, and cleanup. Veil Step does not use the shared
 * Undetectable power: other players see a particle cloud while traversal remains invulnerable
 * and unpushable. No cumulative travel-distance limit is applied.
 */
object VeilStepManager {
    const val SPEED_BLOCKS_PER_SECOND: Double = 15.0
    const val MAX_DURATION_TICKS: Int = 100 // 5 seconds.
    const val COOLDOWN_TICKS: Int = 160 // 8 seconds; Champion recovery makes this 4 seconds.
    const val MANUAL_EXIT_LOCK_TICKS: Int = 4 // Prevent the activating key edge from immediately toggling back out.

     // Vanilla flightSpeed 0.05 is approximately 10.92 blocks/sec. Calibrate the vanilla flight
 // controller to approximately 15 blocks/sec. Sprinting is suppressed while Veil Step is active.
    val VEIL_FLY_SPEED: Float = (0.05 * (VeilStepManager.SPEED_BLOCKS_PER_SECOND / 10.92)).toFloat()

    val PRIMARY: ResourceLocation = VeilStepManager.id("veilborn/primary")
    val VEIL_STEP_MARKER: ResourceLocation = VeilStepManager.id("veilborn/helper/veilstep")
    val UNDETECTABLE: ResourceLocation = VeilStepManager.id("common/undetectable")
    val COOLDOWN: ResourceLocation = VeilStepManager.id("veilborn/primary_veil_step_cooldown")
    val REALITY_SHATTER: ResourceLocation = VeilStepManager.id("veilborn/helper/realityshatter")
    val REALITY_SHATTER_TIMER: ResourceLocation = VeilStepManager.id("veilborn/helper/realityshatter_remove")
    val VEIL_STEP_SOURCE: ResourceLocation = VeilStepManager.id("veil_step")
    val DEBUFF_SOURCE: ResourceLocation = ResourceLocation.fromNamespaceAndPath("minecraft", "debuff")

    val SOUND_OPEN: ResourceLocation = VeilStepManager.id("ability.veilborn.veil_open")
    val SOUND_CLOSE: ResourceLocation = VeilStepManager.id("ability.veilborn.veil_close")
    val SOUND_FRACTURE: ResourceLocation = VeilStepManager.id("ability.veilborn.reality_fracture")

    private val ACTIVE: MutableMap<UUID, VeilStepManager.State> = HashMap<UUID, VeilStepManager.State>()
    private val RESTORING: MutableMap<UUID, RestoreState> = HashMap<UUID, RestoreState>()
    private const val POST_EXIT_RESTORE_TICKS: Int = 10

     // Entity command tags persist with player NBT, unlike ACTIVE/RESTORING. Keeping a tiny snapshot
 // here lets a player who was inside Veil Step during a hard server stop recover their exact
 // pre-Veil flight permissions/state on the next join instead of retaining temporary Veil flight.
    private const val FLIGHT_SNAPSHOT_PREFIX: String = "andromeda_origins.veilstep.flight."
    val FLIGHT_SNAPSHOT_MAYFLY: String = VeilStepManager.FLIGHT_SNAPSHOT_PREFIX + "mayfly"
    val FLIGHT_SNAPSHOT_FLYING: String = VeilStepManager.FLIGHT_SNAPSHOT_PREFIX + "flying"
    val FLIGHT_SNAPSHOT_SPEED_PREFIX: String = VeilStepManager.FLIGHT_SNAPSHOT_PREFIX + "speed."

    fun register() {
        Platform.serverTick(Consumer callback@ { server: MinecraftServer -> if (!VeilStepManager.ACTIVE.isEmpty()) {
            for (uuid in JavaSet.copyOf<UUID>(VeilStepManager.ACTIVE.keys)) {
                val player: ServerPlayer? = server.getPlayerList().getPlayer(uuid)
                if (player == null) {
                    VeilStepManager.ACTIVE.remove(uuid)
                    continue
                }
                VeilStepManager.tick(player)
            }
        }

                    // Re-assert the pre-Veil flight state briefly after exit. This closes the vanilla
            // double-tap-Space window where the client can otherwise keep a stale mayfly/flying
            // state for a few ticks after the Veil marker disappears.
        if (!VeilStepManager.RESTORING.isEmpty()) {
            for (uuid in JavaSet.copyOf<UUID>(VeilStepManager.RESTORING.keys)) {
                val player: ServerPlayer? = server.getPlayerList().getPlayer(uuid)
                val restore: RestoreState? = VeilStepManager.RESTORING.get(uuid)
                if (player == null || restore == null) {
                    VeilStepManager.RESTORING.remove(uuid)
                    continue
                }
                VeilStepManager.enforceRestoredAbilities(player, restore)
                if (--restore.ticksRemaining <= 0) VeilStepManager.RESTORING.remove(uuid)
            }
        }

        })

        Platform.join(JoinCallback callback@ { handler: ServerGamePacketListenerImpl, sender: Any?, server: MinecraftServer -> VeilStepManager.clearStaleState(handler.player)})
        Platform.disconnect(BiConsumer { handler: ServerGamePacketListenerImpl, server: MinecraftServer -> VeilStepManager.RESTORING.remove(handler.player.getUUID())
        val state: VeilStepManager.State? = VeilStepManager.ACTIVE.remove(handler.player.getUUID())
        if (state != null) {
            VeilStepManager.restoreAbilities(handler.player, state, false)
            VeilStepManager.clearPersistentFlightSnapshot(handler.player)
            VeilStepManager.removeVeilPowers(handler.player)
        } else {
                // A clean disconnect should never leave a stale crash-recovery snapshot behind.
            VeilStepManager.clearPersistentFlightSnapshot(handler.player)
        }

        })
    }

    fun isActive(player: ServerPlayer?): Boolean {
        return player != null && VeilStepManager.ACTIVE.containsKey(player.getUUID())
    }

    fun isActive(entity: LivingEntity?): Boolean {
        val player = entity as? ServerPlayer
        return player != null && VeilStepManager.isActive(player)
    }

    fun toggle(player: ServerPlayer?) {
        if (player == null) return
        val existing: VeilStepManager.State? = VeilStepManager.ACTIVE.get(player.getUUID())
        if (existing != null) {
            // Opoli can deliver the activation edge more than once around the state transition.
            // Ignore exit attempts for the first few ticks so one press cannot enter and exit at once.
            if (existing.ticks < VeilStepManager.MANUAL_EXIT_LOCK_TICKS) return
            VeilStepManager.exit(player, existing, ExitReason.MANUAL)
            return
        }
        VeilStepManager.start(player)
    }

    private fun start(player: ServerPlayer) {
        if (!player.isAlive()) return
        VeilStepManager.RESTORING.remove(player.getUUID())
        val container: PowerContainer? = PowerContainer.of(player)
        if (container == null || !container.hasPower(VeilStepManager.PRIMARY) || container.isSuppressed(VeilStepManager.PRIMARY)) return

        if (player.isPassenger()) player.stopRiding()
        val abilities: Abilities = player.getAbilities()
        val state: VeilStepManager.State = VeilStepManager.State(
        player.position(),
        abilities.mayfly,
        abilities.flying,
        abilities.getFlyingSpeed()
        )
        VeilStepManager.storePersistentFlightSnapshot(player, abilities)
        VeilStepManager.ACTIVE.put(player.getUUID(), state)

        container.addPower(VeilStepManager.VEIL_STEP_MARKER, VeilStepManager.VEIL_STEP_SOURCE)
                // Veil Step itself must remain visible even if Auroral Mirage was already active. Suppress
        // only the shared Undetectable presentation for this source; ownership/state is preserved
        // and resumes automatically when Veil Step ends.
        if (container.hasPower(VeilStepManager.UNDETECTABLE)) {
            container.suppressPower(VeilStepManager.UNDETECTABLE, VeilStepManager.VEIL_STEP_SOURCE)
        }

        VeilStepManager.forceFlight(player)
        player.resetFallDistance()

        VeilStepManager.playSound(player, VeilStepManager.SOUND_OPEN, 1.1f, 0.95f, player.position())
        VeilStepManager.tear(player, player.position().add(0.0, player.getBbHeight() * 0.5, 0.0), "veilborn.veil_step_enter", false)
    }

    private fun tick(player: ServerPlayer) {
        val state: VeilStepManager.State? = VeilStepManager.ACTIVE.get(player.getUUID())
        if (state == null) return

        val container: PowerContainer? = PowerContainer.of(player)
        if (!player.isAlive() || container == null || !container.hasPower(VeilStepManager.PRIMARY) || container.isSuppressed(VeilStepManager.PRIMARY)) {
            VeilStepManager.exit(player, state, ExitReason.INVALID_STATE)
            return
        }

                // Keep vanilla spectator-like free-flight controls active. Do not teleport/correct player
        // position: those corrections caused visible rubber-banding in .19.
        VeilStepManager.forceFlight(player)

        val current: Vec3 = player.position()
        if (current.distanceToSqr(state.lastPosition) > 1.0E-8) {
            VeilStepManager.shatterCrossedEntities(player, state, state.lastPosition, current)
            state.lastPosition = current
        }

        // Maintain the cloud while hovering as well as while moving.
        if ((state.ticks and 1) == 0) VeilStepManager.emitVisibleVeilTrail(player)
        state.ticks++
        player.resetFallDistance()

        if (state.ticks >= VeilStepManager.MAX_DURATION_TICKS) {
            VeilStepManager.exit(player, state, ExitReason.TIMEOUT)
        }
    }

    private fun forceFlight(player: ServerPlayer) {
        val abilities: Abilities = player.getAbilities()
        val changed: Boolean = !abilities.mayfly || !abilities.flying || abs(abilities.getFlyingSpeed() - VeilStepManager.VEIL_FLY_SPEED) > 1.0E-5f
        if (!VeilFlightCompat.grant(player)) {
            abilities.mayfly = true
            abilities.flying = true
        }
        abilities.setFlyingSpeed(VeilStepManager.VEIL_FLY_SPEED)
        player.setSprinting(false)
        if (changed) player.onUpdateAbilities()
    }


     /**
 * Broadcasts the Veilborn's shadow-cloud form to nearby clients, including while stationary.
 * Clients suppress the caster's model using the synced Veil Step marker; collision is unchanged.
 */
    private fun emitVisibleVeilTrail(player: ServerPlayer) {
        val level: ServerLevel = player.serverLevel()
        val center: Vec3 = player.position().add(0.0, player.getBbHeight() * 0.52, 0.0)
        val spreadXZ: Double = max(0.18, player.getBbWidth() * 0.42)
        val spreadY: Double = max(0.28, player.getBbHeight() * 0.32)
        level.sendParticles<SimpleParticleType>(ParticleTypes.SMOKE, center.x, center.y, center.z,
        3, spreadXZ, spreadY, spreadXZ, 0.006)
        level.sendParticles<SimpleParticleType>(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
        1, spreadXZ * 0.8, spreadY * 0.8, spreadXZ * 0.8, 0.018)
    }

    private fun shatterCrossedEntities(player: ServerPlayer, state: VeilStepManager.State, fromFeet: Vec3, toFeet: Vec3) {
        val level: ServerLevel = player.serverLevel()
        val halfWidth: Double = player.getBbWidth() * 0.5
        val height: Double = player.getBbHeight().toDouble()
        val swept: AABB = AABB(
        min(fromFeet.x, toFeet.x) - halfWidth,
        min(fromFeet.y, toFeet.y),
        min(fromFeet.z, toFeet.z) - halfWidth,
        max(fromFeet.x, toFeet.x) + halfWidth,
        max(fromFeet.y, toFeet.y) + height,
        max(fromFeet.z, toFeet.z) + halfWidth
        ).inflate(0.1)

        val from: Vec3 = fromFeet.add(0.0, height * 0.5, 0.0)
        val to: Vec3 = toFeet.add(0.0, height * 0.5, 0.0)
        for (target in level.getEntitiesOfClass<LivingEntity>(LivingEntity::class.java, swept,
        Predicate { e: LivingEntity -> e !== player && e.isAlive() && !state.shattered.contains(e.getUUID())})) {
            val expanded: AABB = target.getBoundingBox().inflate(halfWidth, height * 0.5, halfWidth)
            var hit: Vec3? = expanded.clip(from, to).orElse(null)
            if (hit == null && !expanded.contains(to)) continue
            if (hit == null) hit = to

            state.shattered.add(target.getUUID())
            VeilStepManager.applyRealityShatter(target, 5)
            VeilStepManager.tear(player, hit, "veilborn.veil_tear", true)
            VeilStepManager.playSound(player, VeilStepManager.SOUND_FRACTURE, 0.85f, 1.1f, hit)
        }
    }

    private fun exit(player: ServerPlayer, state: VeilStepManager.State, reason: ExitReason?) {
        if (VeilStepManager.ACTIVE.remove(player.getUUID()) == null) return
        VeilStepManager.restoreAbilities(player, state, true)
        VeilStepManager.clearPersistentFlightSnapshot(player)
        VeilStepManager.RESTORING.put(player.getUUID(), RestoreState(
        state.previousMayfly, state.previousFlying, state.previousFlyingSpeed, VeilStepManager.POST_EXIT_RESTORE_TICKS
        ))
        VeilStepManager.removeVeilPowers(player)

                // Drop all residual Veil traversal velocity on emergence. Besides making the endpoint
        // precise, this prevents high-speed collision/kinetic damage from carrying out of Veil Step.
        player.setDeltaMovement(Vec3.ZERO)
        player.resetFallDistance()

        val container: PowerContainer? = PowerContainer.of(player)
        if (reason != ExitReason.INVALID_STATE) {
            if (container != null && container.hasPower(VeilStepManager.COOLDOWN)) {
                PowerResources.write(container, VeilStepManager.COOLDOWN, VeilStepManager.COOLDOWN_TICKS.toDouble())
            }

            val center: Vec3 = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0)
            VeilStepManager.playSound(player, VeilStepManager.SOUND_CLOSE, 1.05f, 1.0f, player.position())
            VeilStepManager.tear(player, center, "veilborn.veil_step_exit", false)

                        // Champion Veilborn intentionally retain their established no-self-debuff rule.
            if (container != null && !AndromedaOriginTags.hasChosenTag(player, AndromedaOriginTags.CHAMPION)) {
                VeilStepManager.applyRealityShatter(player, 5)
            }
        }
    }

    private fun restoreAbilities(player: ServerPlayer, state: VeilStepManager.State, sync: Boolean) {
        val abilities: Abilities = player.getAbilities()
        if (!VeilFlightCompat.release(player)) {
            abilities.mayfly = state.previousMayfly
            abilities.flying = state.previousFlying
        }
        abilities.setFlyingSpeed(state.previousFlyingSpeed)
        if (sync) player.onUpdateAbilities()
    }

    private fun enforceRestoredAbilities(player: ServerPlayer, state: RestoreState) {
        val abilities: Abilities = player.getAbilities()
        if (!VeilFlightCompat.release(player)) {
            abilities.mayfly = state.mayfly
            abilities.flying = state.flying
        }
        abilities.setFlyingSpeed(state.flyingSpeed)
                // Re-send the authoritative pre-Veil state during the short restore window. This prevents
        // stale client double-tap flight without incorrectly forcing legitimate Creative/modded
        // flight off on the client.
        player.onUpdateAbilities()
    }

    private fun removeVeilPowers(player: ServerPlayer?) {
        val container: PowerContainer? = PowerContainer.of(player)
        if (container == null) return
        container.removePower(VeilStepManager.VEIL_STEP_MARKER, VeilStepManager.VEIL_STEP_SOURCE)
        container.unsuppressPower(VeilStepManager.UNDETECTABLE, VeilStepManager.VEIL_STEP_SOURCE)
    }

    private fun clearStaleState(player: ServerPlayer) {
        VeilStepManager.ACTIVE.remove(player.getUUID())
        VeilStepManager.RESTORING.remove(player.getUUID())

        val container: PowerContainer? = PowerContainer.of(player)
        val staleVeilMarker: Boolean = container != null && container.hasPower(VeilStepManager.VEIL_STEP_MARKER)
        val managedFlight = VeilFlightCompat.release(player)
        val restoredSnapshot: Boolean = VeilStepManager.restorePersistentFlightSnapshot(player)

                // Upgrade/recovery fallback for a player saved inside Veil Step by an older build that did
        // not yet persist a pre-Veil snapshot. Only touch non-Creative/non-Spectator flight and only
        // when Andromeda's own stale Veil marker proves the temporary flight state was active.
        if (staleVeilMarker && !restoredSnapshot && !managedFlight && !player.isCreative() && !player.isSpectator()) {
            val abilities: Abilities = player.getAbilities()
            abilities.mayfly = false
            abilities.flying = false
            abilities.setFlyingSpeed(0.05f)
            player.setDeltaMovement(Vec3.ZERO)
            player.resetFallDistance()
            player.onUpdateAbilities()
        }

        VeilStepManager.removeVeilPowers(player)
        VeilStepManager.clearPersistentFlightSnapshot(player)
    }

    private fun storePersistentFlightSnapshot(player: ServerPlayer, abilities: Abilities) {
        VeilStepManager.clearPersistentFlightSnapshot(player)
        if (abilities.mayfly) player.addTag(VeilStepManager.FLIGHT_SNAPSHOT_MAYFLY)
        if (abilities.flying) player.addTag(VeilStepManager.FLIGHT_SNAPSHOT_FLYING)
        player.addTag(VeilStepManager.FLIGHT_SNAPSHOT_SPEED_PREFIX
        + JavaInteger.toUnsignedString(JavaFloat.floatToRawIntBits(abilities.getFlyingSpeed())))
    }

    private fun restorePersistentFlightSnapshot(player: ServerPlayer): Boolean {
        var speedTag: String? = null
        for (tag in player.getTags()) {
            if (tag.startsWith(VeilStepManager.FLIGHT_SNAPSHOT_SPEED_PREFIX)) {
                speedTag = tag
                break
            }
        }
        val hasSnapshot: Boolean = speedTag != null || player.getTags().contains(VeilStepManager.FLIGHT_SNAPSHOT_MAYFLY)
        || player.getTags().contains(VeilStepManager.FLIGHT_SNAPSHOT_FLYING)
        if (!hasSnapshot) return false

        var flyingSpeed: Float = 0.05f
        if (speedTag != null) {
            try {
                val raw: String = speedTag.substring(VeilStepManager.FLIGHT_SNAPSHOT_SPEED_PREFIX.length)
                val decoded: Float = JavaFloat.intBitsToFloat(JavaInteger.parseUnsignedInt(raw))
                if (JavaFloat.isFinite(decoded) && decoded >= 0.0f && decoded <= 1.0f) {
                    flyingSpeed = decoded
                }
            }catch (ignored: NumberFormatException) {
                // Corrupt/stale snapshot data falls back to vanilla's normal flight speed.
            }
        }

        val abilities: Abilities = player.getAbilities()
        if (!VeilFlightCompat.release(player)) {
            abilities.mayfly = player.getTags().contains(VeilStepManager.FLIGHT_SNAPSHOT_MAYFLY)
            abilities.flying = abilities.mayfly && player.getTags().contains(VeilStepManager.FLIGHT_SNAPSHOT_FLYING)
        }
        abilities.setFlyingSpeed(flyingSpeed)
        player.setDeltaMovement(Vec3.ZERO)
        player.resetFallDistance()
        player.onUpdateAbilities()
        VeilStepManager.clearPersistentFlightSnapshot(player)
        return true
    }

    private fun clearPersistentFlightSnapshot(player: ServerPlayer) {
        player.removeTag(VeilStepManager.FLIGHT_SNAPSHOT_MAYFLY)
        player.removeTag(VeilStepManager.FLIGHT_SNAPSHOT_FLYING)
        for (tag in JavaSet.copyOf<String>(player.getTags())) {
            if (tag.startsWith(VeilStepManager.FLIGHT_SNAPSHOT_SPEED_PREFIX)) player.removeTag(tag)
        }
    }

    fun playShimmer(player: ServerPlayer?) {
        if (player == null) return
        val level: ServerLevel = player.serverLevel()
        val center: Vec3 = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0)
        EnhancedFxCompat.playAt(player, "veilborn.shimmer", center)
        level.sendParticles<SimpleParticleType>(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 8, 0.28, 0.45, 0.28, 0.025)
        level.sendParticles<SimpleParticleType>(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 4, 0.2, 0.35, 0.2, 0.01)
        VeilStepManager.playSound(player, VeilStepManager.SOUND_OPEN, 0.35f, 1.6f, center)
    }

    fun applyRealityShatter(target: LivingEntity?, seconds: Int) {
        if (target == null || seconds <= 0) return
        val container: PowerContainer? = PowerContainer.of(target)
        if (container == null) return
        container.addPower(VeilStepManager.REALITY_SHATTER, VeilStepManager.DEBUFF_SOURCE)
        PowerResources.write(container, VeilStepManager.REALITY_SHATTER_TIMER, seconds.toDouble())
    }

    private fun hasUnsuppressed(container: PowerContainer, power: ResourceLocation?): Boolean {
        return container.hasPower(power) && !container.isSuppressed(power)
    }

    private fun tear(source: ServerPlayer, position: Vec3, fx: String, compact: Boolean) {
        EnhancedFxCompat.playAt(source, fx, position)
        val level: ServerLevel = source.serverLevel()
        level.sendParticles<SimpleParticleType>(ParticleTypes.REVERSE_PORTAL, position.x, position.y, position.z,
        if (compact) 4 else 6, if (compact) 0.28 else 0.4, if (compact) 0.45 else 0.65, if (compact) 0.28 else 0.4, 0.035)
        level.sendParticles<SimpleParticleType>(ParticleTypes.DRAGON_BREATH, position.x, position.y, position.z,
        if (compact) 2 else 3, 0.25, 0.4, 0.25, 0.015)
    }

    private fun playSound(source: ServerPlayer, id: ResourceLocation, volume: Float, pitch: Float, pos: Vec3) {
        val sound: SoundEvent? = BuiltInRegistries.SOUND_EVENT.get(id)
        if (sound == null) return
        source.serverLevel().playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch)
    }

    private fun id(path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.CONTENT_NAMESPACE, path)
    }

    private enum class ExitReason {
        MANUAL, TIMEOUT, INVALID_STATE
    }

    private class RestoreState (mayfly: Boolean, flying: Boolean, flyingSpeed: Float, ticksRemaining: Int) {
        val mayfly: Boolean
        val flying: Boolean
        val flyingSpeed: Float
        var ticksRemaining: Int

        init {
            this.mayfly = mayfly
            this.flying = flying
            this.flyingSpeed = flyingSpeed
            this.ticksRemaining = ticksRemaining
        }
    }

    private class State (lastPosition: Vec3, previousMayfly: Boolean, previousFlying: Boolean, previousFlyingSpeed: Float) {
        var lastPosition: Vec3
        var ticks: Int = 0
        val previousMayfly: Boolean
        val previousFlying: Boolean
        val previousFlyingSpeed: Float
        val shattered: MutableSet<UUID> = HashSet<UUID>()

        init {
            this.lastPosition = lastPosition
            this.previousMayfly = previousMayfly
            this.previousFlying = previousFlying
            this.previousFlyingSpeed = previousFlyingSpeed
        }
    }
}
