package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import andromeda.origins.compat.EnhancedFxCompat
import java.util.HashMap
import java.util.HashSet
import andromeda.origins.platform.JoinCallback
import andromeda.origins.compat.AndromedaOriginTags


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
    const val SPEED_BLOCKS_PER_SECOND: kotlin.Double = 15.0
    const val MAX_DURATION_TICKS: kotlin.Int = 100 // 5 seconds.
    const val COOLDOWN_TICKS: kotlin.Int = 160 // 8 seconds; Champion recovery makes this 4 seconds.
    const val MANUAL_EXIT_LOCK_TICKS: kotlin.Int = 4 // Prevent the activating key edge from immediately toggling back out.

     // Vanilla flightSpeed 0.05 is approximately 10.92 blocks/sec. Calibrate the vanilla flight
 // controller to approximately 15 blocks/sec. Sprinting is suppressed while Veil Step is active.
    val VEIL_FLY_SPEED: kotlin.Float = (0.05 * (VeilStepManager.SPEED_BLOCKS_PER_SECOND / 10.92)).toFloat()

    val PRIMARY: net.minecraft.resources.ResourceLocation = VeilStepManager.id("veilborn/primary")
    val VEIL_STEP_MARKER: net.minecraft.resources.ResourceLocation = VeilStepManager.id("veilborn/helper/veilstep")
    val UNDETECTABLE: net.minecraft.resources.ResourceLocation = VeilStepManager.id("common/undetectable")
    val COOLDOWN: net.minecraft.resources.ResourceLocation = VeilStepManager.id("veilborn/primary_veil_step_cooldown")
    val REALITY_SHATTER: net.minecraft.resources.ResourceLocation = VeilStepManager.id("veilborn/helper/realityshatter")
    val REALITY_SHATTER_TIMER: net.minecraft.resources.ResourceLocation = VeilStepManager.id("veilborn/helper/realityshatter_remove")
    val VEIL_STEP_SOURCE: net.minecraft.resources.ResourceLocation = VeilStepManager.id("veil_step")
    val DEBUFF_SOURCE: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "debuff")

    val SOUND_OPEN: net.minecraft.resources.ResourceLocation = VeilStepManager.id("ability.veilborn.veil_open")
    val SOUND_CLOSE: net.minecraft.resources.ResourceLocation = VeilStepManager.id("ability.veilborn.veil_close")
    val SOUND_FRACTURE: net.minecraft.resources.ResourceLocation = VeilStepManager.id("ability.veilborn.reality_fracture")

    private val ACTIVE: kotlin.collections.MutableMap<java.util.UUID, VeilStepManager.State> = HashMap<java.util.UUID, VeilStepManager.State>()
    private val RESTORING: kotlin.collections.MutableMap<java.util.UUID, RestoreState> = HashMap<java.util.UUID, RestoreState>()
    private const val POST_EXIT_RESTORE_TICKS: kotlin.Int = 10

     // Entity command tags persist with player NBT, unlike ACTIVE/RESTORING. Keeping a tiny snapshot
 // here lets a player who was inside Veil Step during a hard server stop recover their exact
 // pre-Veil flight permissions/state on the next join instead of retaining temporary Veil flight.
    private const val FLIGHT_SNAPSHOT_PREFIX: kotlin.String = "andromeda_origins.veilstep.flight."
    val FLIGHT_SNAPSHOT_MAYFLY: kotlin.String = VeilStepManager.FLIGHT_SNAPSHOT_PREFIX + "mayfly"
    val FLIGHT_SNAPSHOT_FLYING: kotlin.String = VeilStepManager.FLIGHT_SNAPSHOT_PREFIX + "flying"
    val FLIGHT_SNAPSHOT_SPEED_PREFIX: kotlin.String = VeilStepManager.FLIGHT_SNAPSHOT_PREFIX + "speed."

    fun register() {
        andromeda.origins.platform.Platform.serverTick(java.util.function.Consumer callback@ { server: net.minecraft.server.MinecraftServer -> if (!VeilStepManager.ACTIVE.isEmpty()) {
            for (uuid in java.util.Set.copyOf<java.util.UUID>(VeilStepManager.ACTIVE.keys)) {
                val player: net.minecraft.server.level.ServerPlayer? = server.getPlayerList().getPlayer(uuid)
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
            for (uuid in java.util.Set.copyOf<java.util.UUID>(VeilStepManager.RESTORING.keys)) {
                val player: net.minecraft.server.level.ServerPlayer? = server.getPlayerList().getPlayer(uuid)
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

        andromeda.origins.platform.Platform.join(JoinCallback callback@ { handler: net.minecraft.server.network.ServerGamePacketListenerImpl, sender: kotlin.Any?, server: net.minecraft.server.MinecraftServer -> VeilStepManager.clearStaleState(handler.player)})
        andromeda.origins.platform.Platform.disconnect(java.util.function.BiConsumer { handler: net.minecraft.server.network.ServerGamePacketListenerImpl, server: net.minecraft.server.MinecraftServer -> VeilStepManager.RESTORING.remove(handler.player.getUUID())
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

    fun isActive(player: net.minecraft.server.level.ServerPlayer?): kotlin.Boolean {
        return player != null && VeilStepManager.ACTIVE.containsKey(player.getUUID())
    }

    fun isActive(entity: net.minecraft.world.entity.LivingEntity?): kotlin.Boolean {
        val player = entity as? net.minecraft.server.level.ServerPlayer
        return player != null && VeilStepManager.isActive(entity)
    }

    fun toggle(player: net.minecraft.server.level.ServerPlayer?) {
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

    private fun start(player: net.minecraft.server.level.ServerPlayer) {
        if (!player.isAlive()) return
        VeilStepManager.RESTORING.remove(player.getUUID())
        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(player)
        if (container == null || !container.hasPower(VeilStepManager.PRIMARY) || container.isSuppressed(VeilStepManager.PRIMARY)) return

        if (player.isPassenger()) player.stopRiding()
        val abilities: net.minecraft.world.entity.player.Abilities = player.getAbilities()
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

    private fun tick(player: net.minecraft.server.level.ServerPlayer) {
        val state: VeilStepManager.State? = VeilStepManager.ACTIVE.get(player.getUUID())
        if (state == null) return

        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(player)
        if (!player.isAlive() || container == null || !container.hasPower(VeilStepManager.PRIMARY) || container.isSuppressed(VeilStepManager.PRIMARY)) {
            VeilStepManager.exit(player, state, ExitReason.INVALID_STATE)
            return
        }

                // Keep vanilla spectator-like free-flight controls active. Do not teleport/correct player
        // position: those corrections caused visible rubber-banding in .19.
        VeilStepManager.forceFlight(player)

        val current: net.minecraft.world.phys.Vec3 = player.position()
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

    private fun forceFlight(player: net.minecraft.server.level.ServerPlayer) {
        val abilities: net.minecraft.world.entity.player.Abilities = player.getAbilities()
        val changed: kotlin.Boolean = !abilities.mayfly || !abilities.flying || kotlin.math.abs(abilities.getFlyingSpeed() - VeilStepManager.VEIL_FLY_SPEED) > 1.0E-5f
        abilities.mayfly = true
        abilities.flying = true
        abilities.setFlyingSpeed(VeilStepManager.VEIL_FLY_SPEED)
        player.setSprinting(false)
        if (changed) player.onUpdateAbilities()
    }


     /**
 * Broadcasts the Veilborn's shadow-cloud form to nearby clients, including while stationary.
 * Clients suppress the caster's model using the synced Veil Step marker; collision is unchanged.
 */
    private fun emitVisibleVeilTrail(player: net.minecraft.server.level.ServerPlayer) {
        val level: net.minecraft.server.level.ServerLevel = player.serverLevel()
        val center: net.minecraft.world.phys.Vec3 = player.position().add(0.0, player.getBbHeight() * 0.52, 0.0)
        val spreadXZ: kotlin.Double = kotlin.math.max(0.18, player.getBbWidth() * 0.42)
        val spreadY: kotlin.Double = kotlin.math.max(0.28, player.getBbHeight() * 0.32)
        level.sendParticles<net.minecraft.core.particles.SimpleParticleType>(net.minecraft.core.particles.ParticleTypes.SMOKE, center.x, center.y, center.z,
        6, spreadXZ, spreadY, spreadXZ, 0.012)
        level.sendParticles<net.minecraft.core.particles.SimpleParticleType>(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
        3, spreadXZ * 0.8, spreadY * 0.8, spreadXZ * 0.8, 0.018)
    }

    private fun shatterCrossedEntities(player: net.minecraft.server.level.ServerPlayer, state: VeilStepManager.State, fromFeet: net.minecraft.world.phys.Vec3, toFeet: net.minecraft.world.phys.Vec3) {
        val level: net.minecraft.server.level.ServerLevel = player.serverLevel()
        val halfWidth: kotlin.Double = player.getBbWidth() * 0.5
        val height: kotlin.Double = player.getBbHeight().toDouble()
        val swept: net.minecraft.world.phys.AABB = net.minecraft.world.phys.AABB(
        kotlin.math.min(fromFeet.x, toFeet.x) - halfWidth,
        kotlin.math.min(fromFeet.y, toFeet.y),
        kotlin.math.min(fromFeet.z, toFeet.z) - halfWidth,
        kotlin.math.max(fromFeet.x, toFeet.x) + halfWidth,
        kotlin.math.max(fromFeet.y, toFeet.y) + height,
        kotlin.math.max(fromFeet.z, toFeet.z) + halfWidth
        ).inflate(0.1)

        val from: net.minecraft.world.phys.Vec3 = fromFeet.add(0.0, height * 0.5, 0.0)
        val to: net.minecraft.world.phys.Vec3 = toFeet.add(0.0, height * 0.5, 0.0)
        for (target in level.getEntitiesOfClass<net.minecraft.world.entity.LivingEntity>(net.minecraft.world.entity.LivingEntity::class.java, swept,
        java.util.function.Predicate { e: net.minecraft.world.entity.LivingEntity -> e !== player && e.isAlive() && !state.shattered.contains(e.getUUID())})) {
            val expanded: net.minecraft.world.phys.AABB = target.getBoundingBox().inflate(halfWidth, height * 0.5, halfWidth)
            var hit: net.minecraft.world.phys.Vec3? = expanded.clip(from, to).orElse(null)
            if (hit == null && !expanded.contains(to)) continue
            if (hit == null) hit = to

            state.shattered.add(target.getUUID())
            VeilStepManager.applyRealityShatter(target, 5)
            VeilStepManager.tear(player, hit, "veilborn.veil_tear", true)
            VeilStepManager.playSound(player, VeilStepManager.SOUND_FRACTURE, 0.85f, 1.1f, hit)
        }
    }

    private fun exit(player: net.minecraft.server.level.ServerPlayer, state: VeilStepManager.State, reason: ExitReason?) {
        if (VeilStepManager.ACTIVE.remove(player.getUUID()) == null) return
        VeilStepManager.restoreAbilities(player, state, true)
        VeilStepManager.clearPersistentFlightSnapshot(player)
        VeilStepManager.RESTORING.put(player.getUUID(), RestoreState(
        state.previousMayfly, state.previousFlying, state.previousFlyingSpeed, VeilStepManager.POST_EXIT_RESTORE_TICKS
        ))
        VeilStepManager.removeVeilPowers(player)

                // Drop all residual Veil traversal velocity on emergence. Besides making the endpoint
        // precise, this prevents high-speed collision/kinetic damage from carrying out of Veil Step.
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO)
        player.resetFallDistance()

        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(player)
        if (reason != ExitReason.INVALID_STATE) {
            if (container != null && container.hasPower(VeilStepManager.COOLDOWN)) {
                dev.overgrown.apoli.power.PowerResources.write(container, VeilStepManager.COOLDOWN, VeilStepManager.COOLDOWN_TICKS)
            }

            val center: net.minecraft.world.phys.Vec3 = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0)
            VeilStepManager.playSound(player, VeilStepManager.SOUND_CLOSE, 1.05f, 1.0f, player.position())
            VeilStepManager.tear(player, center, "veilborn.veil_step_exit", false)

                        // Champion Veilborn intentionally retain their established no-self-debuff rule.
            if (container != null && !AndromedaOriginTags.hasChosenTag(player, AndromedaOriginTags.CHAMPION)) {
                VeilStepManager.applyRealityShatter(player, 5)
            }
        }
    }

    private fun restoreAbilities(player: net.minecraft.server.level.ServerPlayer, state: VeilStepManager.State, sync: kotlin.Boolean) {
        val abilities: net.minecraft.world.entity.player.Abilities = player.getAbilities()
        abilities.mayfly = state.previousMayfly
        abilities.flying = state.previousFlying
        abilities.setFlyingSpeed(state.previousFlyingSpeed)
        if (sync) player.onUpdateAbilities()
    }

    private fun enforceRestoredAbilities(player: net.minecraft.server.level.ServerPlayer, state: RestoreState) {
        val abilities: net.minecraft.world.entity.player.Abilities = player.getAbilities()
        abilities.mayfly = state.mayfly
        abilities.flying = state.flying
        abilities.setFlyingSpeed(state.flyingSpeed)
                // Re-send the authoritative pre-Veil state during the short restore window. This prevents
        // stale client double-tap flight without incorrectly forcing legitimate Creative/modded
        // flight off on the client.
        player.onUpdateAbilities()
    }

    private fun removeVeilPowers(player: net.minecraft.server.level.ServerPlayer?) {
        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(player)
        if (container == null) return
        container.removePower(VeilStepManager.VEIL_STEP_MARKER, VeilStepManager.VEIL_STEP_SOURCE)
        container.unsuppressPower(VeilStepManager.UNDETECTABLE, VeilStepManager.VEIL_STEP_SOURCE)
    }

    private fun clearStaleState(player: net.minecraft.server.level.ServerPlayer) {
        VeilStepManager.ACTIVE.remove(player.getUUID())
        VeilStepManager.RESTORING.remove(player.getUUID())

        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(player)
        val staleVeilMarker: kotlin.Boolean = container != null && container.hasPower(VeilStepManager.VEIL_STEP_MARKER)
        val restoredSnapshot: kotlin.Boolean = VeilStepManager.restorePersistentFlightSnapshot(player)

                // Upgrade/recovery fallback for a player saved inside Veil Step by an older build that did
        // not yet persist a pre-Veil snapshot. Only touch non-Creative/non-Spectator flight and only
        // when Andromeda's own stale Veil marker proves the temporary flight state was active.
        if (staleVeilMarker && !restoredSnapshot && !player.isCreative() && !player.isSpectator()) {
            val abilities: net.minecraft.world.entity.player.Abilities = player.getAbilities()
            abilities.mayfly = false
            abilities.flying = false
            abilities.setFlyingSpeed(0.05f)
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO)
            player.resetFallDistance()
            player.onUpdateAbilities()
        }

        VeilStepManager.removeVeilPowers(player)
        VeilStepManager.clearPersistentFlightSnapshot(player)
    }

    private fun storePersistentFlightSnapshot(player: net.minecraft.server.level.ServerPlayer, abilities: net.minecraft.world.entity.player.Abilities) {
        VeilStepManager.clearPersistentFlightSnapshot(player)
        if (abilities.mayfly) player.addTag(VeilStepManager.FLIGHT_SNAPSHOT_MAYFLY)
        if (abilities.flying) player.addTag(VeilStepManager.FLIGHT_SNAPSHOT_FLYING)
        player.addTag(VeilStepManager.FLIGHT_SNAPSHOT_SPEED_PREFIX
        + java.lang.Integer.toUnsignedString(java.lang.Float.floatToRawIntBits(abilities.getFlyingSpeed())))
    }

    private fun restorePersistentFlightSnapshot(player: net.minecraft.server.level.ServerPlayer): kotlin.Boolean {
        var speedTag: kotlin.String? = null
        for (tag in player.getTags()) {
            if (tag.startsWith(VeilStepManager.FLIGHT_SNAPSHOT_SPEED_PREFIX)) {
                speedTag = tag
                break
            }
        }
        val hasSnapshot: kotlin.Boolean = speedTag != null || player.getTags().contains(VeilStepManager.FLIGHT_SNAPSHOT_MAYFLY)
        || player.getTags().contains(VeilStepManager.FLIGHT_SNAPSHOT_FLYING)
        if (!hasSnapshot) return false

        var flyingSpeed: kotlin.Float = 0.05f
        if (speedTag != null) {
            try {
                val raw: kotlin.String = speedTag.substring(VeilStepManager.FLIGHT_SNAPSHOT_SPEED_PREFIX.length)
                val decoded: kotlin.Float = java.lang.Float.intBitsToFloat(java.lang.Integer.parseUnsignedInt(raw))
                if (java.lang.Float.isFinite(decoded) && decoded >= 0.0f && decoded <= 1.0f) {
                    flyingSpeed = decoded
                }
            }catch (ignored: java.lang.NumberFormatException) {
                // Corrupt/stale snapshot data falls back to vanilla's normal flight speed.
            }
        }

        val abilities: net.minecraft.world.entity.player.Abilities = player.getAbilities()
        abilities.mayfly = player.getTags().contains(VeilStepManager.FLIGHT_SNAPSHOT_MAYFLY)
        abilities.flying = abilities.mayfly && player.getTags().contains(VeilStepManager.FLIGHT_SNAPSHOT_FLYING)
        abilities.setFlyingSpeed(flyingSpeed)
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO)
        player.resetFallDistance()
        player.onUpdateAbilities()
        VeilStepManager.clearPersistentFlightSnapshot(player)
        return true
    }

    private fun clearPersistentFlightSnapshot(player: net.minecraft.server.level.ServerPlayer) {
        player.removeTag(VeilStepManager.FLIGHT_SNAPSHOT_MAYFLY)
        player.removeTag(VeilStepManager.FLIGHT_SNAPSHOT_FLYING)
        for (tag in java.util.Set.copyOf<kotlin.String>(player.getTags())) {
            if (tag.startsWith(VeilStepManager.FLIGHT_SNAPSHOT_SPEED_PREFIX)) player.removeTag(tag)
        }
    }

    fun playShimmer(player: net.minecraft.server.level.ServerPlayer?) {
        if (player == null) return
        val level: net.minecraft.server.level.ServerLevel = player.serverLevel()
        val center: net.minecraft.world.phys.Vec3 = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0)
        EnhancedFxCompat.playAt(player, "veilborn.shimmer", center)
        level.sendParticles<net.minecraft.core.particles.SimpleParticleType>(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 8, 0.28, 0.45, 0.28, 0.025)
        level.sendParticles<net.minecraft.core.particles.SimpleParticleType>(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 4, 0.2, 0.35, 0.2, 0.01)
        VeilStepManager.playSound(player, VeilStepManager.SOUND_OPEN, 0.35f, 1.6f, center)
    }

    fun applyRealityShatter(target: net.minecraft.world.entity.LivingEntity?, seconds: kotlin.Int) {
        if (target == null || seconds <= 0) return
        val container: dev.overgrown.apoli.power.PowerContainer? = dev.overgrown.apoli.power.PowerContainer.of(target)
        if (container == null) return
        container.addPower(VeilStepManager.REALITY_SHATTER, VeilStepManager.DEBUFF_SOURCE)
        dev.overgrown.apoli.power.PowerResources.write(container, VeilStepManager.REALITY_SHATTER_TIMER, seconds)
    }

    private fun hasUnsuppressed(container: dev.overgrown.apoli.power.PowerContainer, power: net.minecraft.resources.ResourceLocation?): kotlin.Boolean {
        return container.hasPower(power) && !container.isSuppressed(power)
    }

    private fun tear(source: net.minecraft.server.level.ServerPlayer, position: net.minecraft.world.phys.Vec3, fx: kotlin.String, compact: kotlin.Boolean) {
        EnhancedFxCompat.playAt(source, fx, position)
        val level: net.minecraft.server.level.ServerLevel = source.serverLevel()
        level.sendParticles<net.minecraft.core.particles.SimpleParticleType>(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, position.x, position.y, position.z,
        if (compact) 10 else 18, if (compact) 0.28 else 0.4, if (compact) 0.45 else 0.65, if (compact) 0.28 else 0.4, 0.035)
        level.sendParticles<net.minecraft.core.particles.SimpleParticleType>(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, position.x, position.y, position.z,
        if (compact) 4 else 7, 0.25, 0.4, 0.25, 0.015)
    }

    private fun playSound(source: net.minecraft.server.level.ServerPlayer, id: net.minecraft.resources.ResourceLocation, volume: kotlin.Float, pitch: kotlin.Float, pos: net.minecraft.world.phys.Vec3) {
        val sound: net.minecraft.sounds.SoundEvent? = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id)
        if (sound == null) return
        source.serverLevel().playSound(null, pos.x, pos.y, pos.z, sound, net.minecraft.sounds.SoundSource.PLAYERS, volume, pitch)
    }

    private fun id(path: kotlin.String): net.minecraft.resources.ResourceLocation {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.CONTENT_NAMESPACE, path)
    }

    private enum class ExitReason {
        MANUAL, TIMEOUT, INVALID_STATE
    }

    private class RestoreState (mayfly: kotlin.Boolean, flying: kotlin.Boolean, flyingSpeed: kotlin.Float, ticksRemaining: kotlin.Int) {
        val mayfly: kotlin.Boolean
        val flying: kotlin.Boolean
        val flyingSpeed: kotlin.Float
        var ticksRemaining: kotlin.Int

        init {
            this.mayfly = mayfly
            this.flying = flying
            this.flyingSpeed = flyingSpeed
            this.ticksRemaining = ticksRemaining
        }
    }

    private class State (lastPosition: net.minecraft.world.phys.Vec3, previousMayfly: kotlin.Boolean, previousFlying: kotlin.Boolean, previousFlyingSpeed: kotlin.Float) {
        var lastPosition: net.minecraft.world.phys.Vec3
        var ticks: kotlin.Int = 0
        val previousMayfly: kotlin.Boolean
        val previousFlying: kotlin.Boolean
        val previousFlyingSpeed: kotlin.Float
        val shattered: kotlin.collections.MutableSet<java.util.UUID> = HashSet<java.util.UUID>()

        init {
            this.lastPosition = lastPosition
            this.previousMayfly = previousMayfly
            this.previousFlying = previousFlying
            this.previousFlyingSpeed = previousFlyingSpeed
        }
    }
}
