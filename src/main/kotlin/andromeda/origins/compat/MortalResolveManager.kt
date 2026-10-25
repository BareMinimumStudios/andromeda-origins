package andromeda.origins.compat

import andromeda.origins.compat.IncapacitatedCompat
import andromeda.origins.compat.EnhancedFxCompat
import andromeda.origins.compat.FiguraCompatManager
import java.util.HashMap
import andromeda.origins.platform.RespawnCallback
import andromeda.origins.compat.MortalResolveEffects
import java.util.HashSet
import andromeda.origins.platform.DamageCallback
import andromeda.origins.platform.JoinCallback


 /**
 * Server-side lifetime tracking for Humanity's Mortal Resolve final stand.
 *
 * Origin storage preserves the world-time deadline across reconnects and server restarts.
 * Death and Origin cleanup clear it so a copied countdown cannot kill a respawned player.
 * A command tag mirrors the active state while the runtime maps drive per-tick effects.
 */
object MortalResolveManager {
    const val ACTIVE_TAG: kotlin.String = "andromeda_mortal_resolve"
    const val CHAMPION_ACTIVE_TAG: kotlin.String = "andromeda_champion_mortal_resolve"
    private const val FINAL_DEATH_PENDING_TAG: kotlin.String = "andromeda_mortal_resolve_restore_incap"
    val DURATION_TICKS: kotlin.Int = 20 * 60

    val ACTIVE: kotlin.collections.MutableMap<java.util.UUID, kotlin.Int> = HashMap<java.util.UUID, kotlin.Int>()
    val CHAMPION_ACTIVE: kotlin.collections.MutableMap<java.util.UUID, kotlin.Int> = HashMap<java.util.UUID, kotlin.Int>()
    val FINAL_DEATH_PENDING: kotlin.collections.MutableSet<java.util.UUID> = HashSet<java.util.UUID>()

    private const val DEADLINE_KEY: kotlin.String = "andromeda_mortal_resolve_deadline"
    private const val CHAMPION_DEADLINE_KEY: kotlin.String = "andromeda_champion_mortal_resolve_deadline"

    private fun remaining(player: net.minecraft.server.level.ServerPlayer, key: kotlin.String?): kotlin.Int {
        try {
            val value: kotlin.String? = dev.overgrown.origins.storage.OriginStorage.value(player, key)
            if (value == null) return 0
            return kotlin.math.max(0L, kotlin.math.min(MortalResolveManager.DURATION_TICKS.toLong(), value.toLong() - player.server.overworld().getGameTime())).toInt()
        }catch (exception: java.lang.NumberFormatException) {
            return 0
        }
    }

    fun register() {
        andromeda.origins.platform.Platform.serverStopped(java.util.function.Consumer callback@ { server: net.minecraft.server.MinecraftServer -> MortalResolveManager.ACTIVE.clear()
        MortalResolveManager.CHAMPION_ACTIVE.clear()
        MortalResolveManager.FINAL_DEATH_PENDING.clear()

        })
        andromeda.origins.platform.Platform.disconnect { handler, _ ->
            // Tags and stored deadlines retain offline finality; runtime effects need no offline entry.
            ACTIVE.remove(handler.player.uuid)
            CHAMPION_ACTIVE.remove(handler.player.uuid)
            FINAL_DEATH_PENDING.remove(handler.player.uuid)
        }
        andromeda.origins.platform.Platform.serverTick(java.util.function.Consumer callback@ { server: net.minecraft.server.MinecraftServer ->
            // Only active final-stand entries are visited here. Restart recovery is handled by the
            // player-join event below, avoiding an all-online-player scan every server tick.
        val iterator: kotlin.collections.MutableIterator<kotlin.collections.MutableMap.MutableEntry<java.util.UUID, kotlin.Int>> = MortalResolveManager.ACTIVE.entries.iterator()
        while (iterator.hasNext()) {
            val entry: kotlin.collections.MutableMap.MutableEntry<java.util.UUID, kotlin.Int> = iterator.next()
            val player: net.minecraft.server.level.ServerPlayer? = server.getPlayerList().getPlayer(entry.key)
            val remaining: kotlin.Int = entry.value

                            // Time continues to pass while disconnected. Once the player returns, an expired
                // final stand resolves immediately instead of allowing logout to avoid the death.
            if (player == null) {
                iterator.remove()
                continue
            }

                            // A real death before the timer expires already fulfills Mortal Resolve's finality.
                // The respawn callback below also clears the mirrored command tag.
            if (!player.isAlive()) {
                dev.overgrown.origins.storage.OriginStorage.clear(player, DEADLINE_KEY)
                player.removeTag(MortalResolveManager.ACTIVE_TAG)
                iterator.remove()
                continue
            }

            if (!player.getTags().contains(MortalResolveManager.ACTIVE_TAG)) {
                dev.overgrown.origins.storage.OriginStorage.clear(player, DEADLINE_KEY)
                iterator.remove()
                continue
            }

                            // Let the initial burst breathe for a moment, then leave a sparse upward trail
                // for the first 1.5 seconds. Five-tick spacing keeps this cosmetic effect cheap.
            val elapsed: kotlin.Int = MortalResolveManager.DURATION_TICKS - remaining
            if (elapsed >= 5 && elapsed <= 30 && elapsed % 5 == 0) {
                MortalResolveEffects.playAfterglow(player)
            }

                            // Refresh the short custom heartbeat at the Human's current position every 1.5s.
                // This avoids the old long positional sound becoming inaudible as the player moved.
            if (elapsed > 0 && elapsed % 30 == 0) {
                MortalResolveEffects.playHeartbeatPulse(player)
            }

                            // Cosmetic escalation: the borrowed minute grows increasingly unstable as it runs out.
            if (remaining == 800) {
                EnhancedFxCompat.play(player, "humanity.mortal_resolve_stage_1")
            } else if (remaining == 400) {
                EnhancedFxCompat.play(player, "humanity.mortal_resolve_stage_2")
            } else if (remaining == 200) {
                EnhancedFxCompat.play(player, "humanity.mortal_resolve_stage_3")
            }

            if (remaining <= 1) {
                iterator.remove()
                dev.overgrown.origins.storage.OriginStorage.clear(player, DEADLINE_KEY)
                MortalResolveManager.FINAL_DEATH_PENDING.add(player.getUUID())
                player.addTag(MortalResolveManager.FINAL_DEATH_PENDING_TAG)
                                    // Keep ACTIVE_TAG present until the death attempt has been evaluated.
                    // Indomitable and other standard-Human finality guards key off this tag;
                    // removing it before player.kill() reopened prevent-death for one call.
                IncapacitatedCompat.forceMortalResolveDeath(player)
            } else {
                entry.setValue(remaining - 1)
            }
        }

        val championIterator: kotlin.collections.MutableIterator<kotlin.collections.MutableMap.MutableEntry<java.util.UUID, kotlin.Int>> = MortalResolveManager.CHAMPION_ACTIVE.entries.iterator()
        while (championIterator.hasNext()) {
            val entry: kotlin.collections.MutableMap.MutableEntry<java.util.UUID, kotlin.Int> = championIterator.next()
            val player: net.minecraft.server.level.ServerPlayer? = server.getPlayerList().getPlayer(entry.key)
            val remaining: kotlin.Int = entry.value

            if (player == null) {
                championIterator.remove()
                continue
            }

            if (!player.isAlive() || !player.getTags().contains(MortalResolveManager.CHAMPION_ACTIVE_TAG)) {
                dev.overgrown.origins.storage.OriginStorage.clear(player, CHAMPION_DEADLINE_KEY)
                player.removeTag(MortalResolveManager.CHAMPION_ACTIVE_TAG)
                MortalResolveEffects.stopHeartbeat(player)
                championIterator.remove()
                continue
            }

            val elapsed: kotlin.Int = MortalResolveManager.DURATION_TICKS - remaining
            if (elapsed >= 5 && elapsed <= 30 && elapsed % 5 == 0) {
                MortalResolveEffects.playAfterglow(player)
            }
            if (elapsed > 0 && elapsed % 30 == 0) {
                MortalResolveEffects.playHeartbeatPulse(player)
            }
            if (remaining == 800) {
                EnhancedFxCompat.play(player, "humanity.mortal_resolve_stage_1")
            } else if (remaining == 400) {
                EnhancedFxCompat.play(player, "humanity.mortal_resolve_stage_2")
            } else if (remaining == 200) {
                EnhancedFxCompat.play(player, "humanity.mortal_resolve_stage_3")
            }

            if (remaining <= 1) {
                championIterator.remove()
                dev.overgrown.origins.storage.OriginStorage.clear(player, CHAMPION_DEADLINE_KEY)
                player.removeTag(MortalResolveManager.CHAMPION_ACTIVE_TAG)
                MortalResolveEffects.stopHeartbeat(player)
            } else {
                entry.setValue(remaining - 1)
            }
        }

        })

                // Fabric fires this before ServerPlayer's death processing. Preparing Incapacitated's
        // counter here means its own death hook sees Mortal Resolve deaths as final without needing
        // a cross-mod Mixin-order dependency.
        andromeda.origins.platform.Platform.death(DamageCallback callback@ {
        entity: net.minecraft.world.entity.LivingEntity, damageSource: net.minecraft.world.damagesource.DamageSource, damageAmount: kotlin.Float ->
         if (entity !is net.minecraft.server.level.ServerPlayer) {
            return@callback true
        }
        if (entity.getTags().contains(MortalResolveManager.ACTIVE_TAG)) {
            MortalResolveEffects.stopHeartbeat(entity)
            MortalResolveManager.FINAL_DEATH_PENDING.add(entity.getUUID())
            entity.addTag(MortalResolveManager.FINAL_DEATH_PENDING_TAG)
            IncapacitatedCompat.prepareMortalResolveDeath(entity)
        }
        if (entity.getTags().contains(MortalResolveManager.CHAMPION_ACTIVE_TAG)) {
            MortalResolveManager.stopChampion(entity)
        }
        true
        })

        andromeda.origins.platform.Platform.respawn(RespawnCallback callback@ { oldPlayer: net.minecraft.server.level.ServerPlayer, newPlayer: net.minecraft.server.level.ServerPlayer, alive: kotlin.Boolean -> val mortalResolveDeath: kotlin.Boolean = MortalResolveManager.FINAL_DEATH_PENDING.remove(oldPlayer.getUUID())
        || oldPlayer.getTags().contains(MortalResolveManager.ACTIVE_TAG)
        || oldPlayer.getTags().contains(MortalResolveManager.FINAL_DEATH_PENDING_TAG)
        || newPlayer.getTags().contains(MortalResolveManager.FINAL_DEATH_PENDING_TAG)
        oldPlayer.removeTag(MortalResolveManager.FINAL_DEATH_PENDING_TAG)
        newPlayer.removeTag(MortalResolveManager.FINAL_DEATH_PENDING_TAG)
        MortalResolveManager.stop(newPlayer)
        MortalResolveManager.stopChampion(newPlayer)
        if (mortalResolveDeath) {
            IncapacitatedCompat.restorePostMortalResolveState(newPlayer)
        } else {
            IncapacitatedCompat.repairUnlimitedDownCounter(newPlayer)
        }

        })

                // Normalize Incapacitated's unlimited-down sentinel when the server play handler is ready. Using the long-lived
        // networking join event keeps this compatible with the project's Fabric API baseline.
        andromeda.origins.platform.Platform.join(JoinCallback callback@ { handler: net.minecraft.server.network.ServerGamePacketListenerImpl, sender: kotlin.Any?, server: net.minecraft.server.MinecraftServer -> val player: net.minecraft.server.level.ServerPlayer = handler.getPlayer()
        if (player.getTags().contains(MortalResolveManager.FINAL_DEATH_PENDING_TAG)) {
            player.removeTag(MortalResolveManager.FINAL_DEATH_PENDING_TAG)
            MortalResolveManager.stop(player)
            IncapacitatedCompat.restorePostMortalResolveState(player)
            return@callback
        }

                    // Command tags survive a normal server restart while the in-memory timer maps do not.
            // Re-arm only this joining player instead of scanning every online player every tick.
        if (player.getTags().contains(MortalResolveManager.ACTIVE_TAG) && !MortalResolveManager.ACTIVE.containsKey(player.getUUID())) {
            MortalResolveManager.ACTIVE.put(player.getUUID(), MortalResolveManager.remaining(player, MortalResolveManager.DEADLINE_KEY))
        }
        if (player.getTags().contains(MortalResolveManager.CHAMPION_ACTIVE_TAG)
        && !MortalResolveManager.CHAMPION_ACTIVE.containsKey(player.getUUID())) {
            MortalResolveManager.CHAMPION_ACTIVE.put(player.getUUID(), MortalResolveManager.remaining(player, MortalResolveManager.CHAMPION_DEADLINE_KEY))
        }

        if (!MortalResolveManager.isActive(player)) {
            IncapacitatedCompat.repairUnlimitedDownCounter(player)
        }

        })
    }

    fun isActive(player: net.minecraft.server.level.ServerPlayer): kotlin.Boolean {
        return MortalResolveManager.ACTIVE.containsKey(player.getUUID()) || player.getTags().contains(MortalResolveManager.ACTIVE_TAG)
    }

    fun start(player: net.minecraft.server.level.ServerPlayer) {
        dev.overgrown.origins.storage.OriginStorage.storeValue(player, MortalResolveManager.DEADLINE_KEY, (player.server.overworld().getGameTime() + MortalResolveManager.DURATION_TICKS).toString())
        MortalResolveManager.ACTIVE.put(player.getUUID(), MortalResolveManager.DURATION_TICKS)
        player.addTag(MortalResolveManager.ACTIVE_TAG)
        FiguraCompatManager.setResource(player, FiguraCompatManager.MORTAL_RESOLVE_RESOURCE, 1)
    }

    fun isChampionActive(player: net.minecraft.server.level.ServerPlayer): kotlin.Boolean {
        return MortalResolveManager.CHAMPION_ACTIVE.containsKey(player.getUUID())
        || player.getTags().contains(MortalResolveManager.CHAMPION_ACTIVE_TAG)
    }

    fun startChampion(player: net.minecraft.server.level.ServerPlayer) {
        dev.overgrown.origins.storage.OriginStorage.storeValue(player, MortalResolveManager.CHAMPION_DEADLINE_KEY, (player.server.overworld().getGameTime() + MortalResolveManager.DURATION_TICKS).toString())
        MortalResolveManager.CHAMPION_ACTIVE.put(player.getUUID(), MortalResolveManager.DURATION_TICKS)
        player.addTag(MortalResolveManager.CHAMPION_ACTIVE_TAG)
        FiguraCompatManager.setResource(player, FiguraCompatManager.MORTAL_RESOLVE_RESOURCE, 1)
    }

    fun stopChampion(player: net.minecraft.server.level.ServerPlayer) {
        if (MortalResolveManager.CHAMPION_ACTIVE.containsKey(player.getUUID())
        || player.getTags().contains(MortalResolveManager.CHAMPION_ACTIVE_TAG)) {
            MortalResolveEffects.stopHeartbeat(player)
        }
        dev.overgrown.origins.storage.OriginStorage.clear(player, MortalResolveManager.CHAMPION_DEADLINE_KEY)
        MortalResolveManager.CHAMPION_ACTIVE.remove(player.getUUID())
        player.removeTag(MortalResolveManager.CHAMPION_ACTIVE_TAG)
        FiguraCompatManager.setResource(player, FiguraCompatManager.MORTAL_RESOLVE_RESOURCE, 0)
    }

    fun stop(player: net.minecraft.server.level.ServerPlayer) {
        if (MortalResolveManager.ACTIVE.containsKey(player.getUUID()) || player.getTags().contains(MortalResolveManager.ACTIVE_TAG)) {
            MortalResolveEffects.stopHeartbeat(player)
        }
        dev.overgrown.origins.storage.OriginStorage.clear(player, MortalResolveManager.DEADLINE_KEY)
        MortalResolveManager.ACTIVE.remove(player.getUUID())
        player.removeTag(MortalResolveManager.ACTIVE_TAG)
        FiguraCompatManager.setResource(player, FiguraCompatManager.MORTAL_RESOLVE_RESOURCE, 0)
    }
}
