package andromeda.origins.compat

import andromeda.origins.platform.Platform
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import dev.overgrown.apoli.PowerContainerAttachment
import dev.overgrown.apoli.action.ActionType
import dev.overgrown.apoli.action.ActionTypes
import dev.overgrown.apoli.condition.context.EntityCtx
import dev.overgrown.apoli.power.PowerContainer
import dev.overgrown.apoli.power.PowerResources
import dev.overgrown.apoli.power.builtin.AttributePower
import dev.overgrown.origins.component.PlayerOriginsAttachment
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.RelativeMovement
import net.minecraft.world.item.Item
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.util.UUID
import kotlin.math.*

/** Champion-only casts. Persistent costs live in native resource powers; world mutation stays on the server. */
object Dawnbreaker {
    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath("andromeda_origins", path)
    private val origin = id("dawnbreaker")
    private val radiance = id("dawnbreaker/radiance")
    private val stoneAppearance = id("dawnbreaker/stone_appearance")
    private val source = id("dawnbreaker_effect")
    private val cc = ResourceLocation.parse("minecraft:ccontrol")
    private val gaze = id("dawnbreaker/primary")
    private val aura = id("dawnbreaker/presence")
    private val bow = id("dawnbreaker/bow")
    private val held = mutableMapOf<UUID, Int>()
    private val begone = id("dawnbreaker/begone")
    private val stone = id("dawnbreaker/helper/stone")
    private val petrify = id("dawnbreaker/helper/ophidianed")
    private val oppressed = id("dawnbreaker/helper/oppressed")
    private val pumpkin = TagKey.create(Registries.ITEM, ResourceLocation.parse("origins:pumpkin_like/equipment"))
    private val gold = DustParticleOptions(Vector3f(1f, .78f, .23f), .25f)
    private val jade = DustParticleOptions(Vector3f(.32f, .81f, .55f), .22f)
    private val affected = mutableSetOf<LivingEntity>()
    private data class Statue(val owners: MutableMap<UUID, Double>, var lastTimer: Double)
    private val statues = mutableMapOf<LivingEntity, Statue>()
    private fun syncStatue(target: LivingEntity, state: Statue) {
        val timer = resource(target, petrify, "remove")
        val elapsed = (state.lastTimer - timer).coerceAtLeast(0.0)
        if (elapsed > 0) state.owners.replaceAll { _, remaining -> (remaining - elapsed).coerceAtLeast(0.0) }
        state.owners.entries.removeIf { it.value <= 0 }
        state.lastTimer = timer
    }
    private fun publishStatue(target: LivingEntity, state: Statue) {
        val timer = state.owners.values.maxOrNull() ?: 0.0
        set(target, petrify, "remove", timer); state.lastTimer = timer
    }

    fun registerActions() {
        ActionTypes.ENTITY.register(id("dawnbreaker"), object : ActionType<EntityCtx, String> {
            override fun codec(): MapCodec<String> = Codec.STRING.fieldOf("kind")
            override fun run(cfg: String, ctx: EntityCtx) {
                val player = ctx.raw() as? ServerPlayer ?: return
                when (cfg) {
                    "gaze" -> castGaze(player)
                    "transference" -> castTransference(player)
                    "measured_step" -> measuredStep(player)
                    "dawnbreaker" -> castAura(player)
                    "begone" -> castBanish(player)
                    "bow" -> holdBow(player)
                    "kneel" -> SovereignArts.kneel(player)
                    "stillwater" -> SovereignArts.hold(player)
                }
            }
        })
    }

    internal fun owns(player: ServerPlayer, power: ResourceLocation): Boolean {
        val c = PowerContainer.of(player) ?: return false
        return player.isAlive && !player.isSpectator &&
            PlayerOriginsAttachment.get(player)?.snapshot()?.containsValue(origin) == true &&
            c.hasPower(power) && !c.isSuppressed(power)
    }
    internal fun usable(player: ServerPlayer, power: ResourceLocation): Boolean {
        val c = PowerContainer.of(player) ?: return false
        return owns(player, power) && !IncapacitatedCompat.isDowned(player) &&
            !(c.hasPower(id("common/silenced")) && !c.isSuppressed(id("common/silenced"))) &&
            !VeilStepManager.isActive(player)
    }
    private fun resource(player: LivingEntity, root: ResourceLocation, suffix: String): Double =
        PowerContainer.of(player)?.let { PowerResources.read(it, id(root.path + "_" + suffix)).orElse(0.0) } ?: 0.0
    private fun set(player: LivingEntity, root: ResourceLocation, suffix: String, value: Double) {
        PowerContainer.of(player)?.let {
            val key = id(root.path + "_" + suffix)
            if (PowerResources.read(it, key).orElse(value) != value) { PowerResources.write(it, key, value); it.markDirty() }
        }
    }
    private fun say(player: ServerPlayer, text: String) = player.displayClientMessage(Component.literal(text), true)
    internal fun success(player: ServerPlayer, text: String) {
        if (DawnbreakerPresentation.settings().successMessages.get()) say(player, text)
    }
    private fun ally(actor: LivingEntity, target: LivingEntity): Boolean {
        if (actor === target) return true
        // Rank/nameplate teams may intentionally allow PvP; membership alone is not protection.
        if (actor is net.minecraft.world.entity.player.Player && target is net.minecraft.world.entity.player.Player) {
            val team = actor.team
            if (team != null && team.isAlliedTo(target.team)) return !team.isAllowFriendlyFire
        }
        if (actor.isAlliedTo(target) || target.isAlliedTo(actor)) return true
        if (target is TamableAnimal) {
            if (target.ownerUUID == actor.uuid) return true
            if (target.owner?.let { actor.isAlliedTo(it) || it.isAlliedTo(actor) } == true) return true
        }
        return false
    }
    internal fun validTarget(actor: ServerPlayer, target: LivingEntity): Boolean = target.isAlive &&
        !target.isSpectator && target !== actor && target.level() === actor.level() && !ally(actor, target) &&
        !(target is ServerPlayer && !actor.canHarmPlayer(target)) &&
        !(PowerContainer.of(target)?.let { it.hasPower(id("common/unstoppable")) && !it.isSuppressed(id("common/unstoppable")) } ?: false)

    private fun gazeBlockReason(actor: ServerPlayer, target: LivingEntity?): String = when {
        target == null -> "No visible target in your gaze: look within 24 blocks and the forward cone."
        target.getItemBySlot(EquipmentSlot.HEAD).`is`(pumpkin) -> "Gaze blocked: target has pumpkin protection."
        target is ServerPlayer && !actor.server.isPvpAllowed -> "Gaze blocked: server PvP is disabled."
        ally(actor, target) -> "Gaze blocked: target is an ally or protected teammate."
        target is ServerPlayer && !actor.canHarmPlayer(target) -> "Gaze blocked: player PvP or friendly-fire protection."
        PowerContainer.of(target)?.let { it.hasPower(id("common/unstoppable")) && !it.isSuppressed(id("common/unstoppable")) } == true -> "Gaze blocked: target has Unstoppable."
        else -> "Gaze blocked: target is protected."
    }
    private fun measuredStep(player: ServerPlayer) {
        if (!usable(player, id("dawnbreaker/measured_step"))) return
        if (player.isPassenger || player.isVehicle) { say(player, "Measured Step — dismount and release passengers first."); return }
        val level = player.serverLevel()
        val result = MeasuredStepLanding.find(player)
        val destination = result.position ?: run { say(player, result.reason); return }
        val from = player.position()
        if (!player.teleportTo(level, destination.x, destination.y, destination.z, mutableSetOf<RelativeMovement>(), player.yRot, player.xRot)) return
        player.deltaMovement = Vec3.ZERO; player.fallDistance = 0f
        sound(level, from, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, .32f, 1.7f)
        DawnbreakerFx.cue(player, 0, from)
        set(player, id("dawnbreaker/presentation"), "step_x", from.x)
        set(player, id("dawnbreaker/presentation"), "step_y", from.y)
        set(player, id("dawnbreaker/presentation"), "step_z", from.z)
        set(player, id("dawnbreaker/presentation"), "step", 2.0)
        // The move itself is a same-tick teleport. Only mark the endpoints, never draw a travel path.
        if (radiant(player)) for (p in listOf(from, destination)) {
            level.sendParticles(gold, p.x, p.y + 1.0, p.z, DawnbreakerPresentation.particles(3), .08, .18, .08, 0.0)
        }
    }

    private fun castTransference(player: ServerPlayer) {
        if (!usable(player, id("dawnbreaker/secondary"))) return
        val eye = player.eyePosition
        val end = eye.add(player.lookAngle.scale(32.0))
        val wall = player.serverLevel().clip(ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
        val target = player.serverLevel().getEntitiesOfClass(LivingEntity::class.java, player.boundingBox.expandTowards(player.lookAngle.scale(32.0)).inflate(1.0)) {
            validTarget(player, it) && !it.getItemBySlot(EquipmentSlot.HEAD).`is`(pumpkin)
        }.mapNotNull { e -> e.boundingBox.inflate(.15).clip(eye, end).orElse(null)?.let { e to it } }
            .filter { eye.distanceToSqr(it.second) <= eye.distanceToSqr(wall.location) }
            .minByOrNull { eye.distanceToSqr(it.second) }?.first
        if (target == null) { say(player, "Transference — look directly at an eligible target within 32 blocks."); return }
        DawnbreakerTransference.apply(player, target)
        PowerContainerAttachment.getOrCreate(player)?.let { c ->
            val cue = id("gorgon/helper/intimidating")
            c.addPower(cue, ResourceLocation.parse("minecraft:state")); set(player, cue, "remove", 1.0)
            set(player, id("common/figura"), "2", 1.0); c.markDirty()
        }
        if (radiant(player)) {
            val ray = target.eyePosition.subtract(eye)
            val samples = DawnbreakerPresentation.particles(20)
            for (i in 1..samples) {
                val at = eye.add(ray.scale(i.toDouble() / samples))
                player.serverLevel().sendParticles(gold, at.x, at.y, at.z, 1, .015, .015, .015, 0.0)
            }
        }
        sound(player.serverLevel(), player.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, .22f, .8f)
        success(player, "Transference.")
    }
    private fun castAura(player: ServerPlayer) {
        if (!usable(player, aura)) return
        val refreshing = resource(player, aura, "active") > 0
        set(player, aura, "active", 400.0)
        set(player, aura, "cooldown", 0.0)
        if (!refreshing) sound(player.serverLevel(), player.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, .18f, .65f)
        success(player, "Dawnbreaker’s Presence — 20 seconds")
    }
    private fun castGaze(player: ServerPlayer) {
        if (!usable(player, gaze)) return
        set(player, gaze, "charges", 2.0)
        set(player, gaze, "recharge", 0.0)
        val eye = player.eyePosition
        val look = player.lookAngle
        val visibleTargets = player.serverLevel().getEntitiesOfClass(LivingEntity::class.java, player.boundingBox.inflate(24.0)) {
            it !== player && it.isAlive && !it.isSpectator &&
                it.distanceToSqr(player) <= 24.0 * 24.0 && player.hasLineOfSight(it) &&
                look.dot(it.eyePosition.subtract(eye).normalize()) >= cos(Math.toRadians(30.0))
        }.sortedWith(compareByDescending<LivingEntity> { look.dot(it.eyePosition.subtract(eye).normalize()) }.thenBy { it.distanceToSqr(player) })
        val targets = visibleTargets.filter { validTarget(player, it) && !it.getItemBySlot(EquipmentSlot.HEAD).`is`(pumpkin) }.take(5)
        if (targets.isEmpty()) { say(player, gazeBlockReason(player, visibleTargets.firstOrNull())); return }
        for (target in targets) {
            val c = PowerContainerAttachment.getOrCreate(target) ?: continue
            val state = statues.getOrPut(target) { Statue(mutableMapOf(), resource(target, petrify, "remove")) }
            syncStatue(target, state)
            state.owners[player.uuid] = ((state.owners[player.uuid] ?: 0.0) + 30.0).coerceAtMost(60.0)
            c.addPower(petrify, cc)
            publishStatue(target, state)
            if (owns(player, stoneAppearance)) {
                val beginning = source !in c.sourcesOf(stone)
                c.addPower(stone, source)
                if (beginning) set(target, stone, "progress", 0.0)
                set(target, stone, "gradual", if (owns(player, id("dawnbreaker/gradual_petrification"))) 1.0 else 0.0)
            }
            c.markDirty()
            if (radiant(player)) shards(target)
        }
        sound(player.serverLevel(), player.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, .7f, .6f)
        ring(player, player.serverLevel(), player.position(), 2.0, 12)
        success(player, "Sovereign Gaze — ${targets.size} petrified")
    }

    private val lightBowAvailable by lazy {
        Platform.isModLoaded("simplyswords") && try { Class.forName("net.sweenus.simplyswords.entity.DawnquiverBowVisualEntity"); true }
        catch (_: ReflectiveOperationException) { false } catch (_: LinkageError) { false }
    }
    private fun holdBow(player: ServerPlayer) {
        if (!lightBowAvailable) { say(player, "Seraph’s Draw requires Simply Swords with Dawnquiver support."); return }

        if (!usable(player, bow)) return
        set(player, bow, "cooldown", 0.0)
        if (player.uuid !in held) bowSound(player.serverLevel(), player.position(), "magic_bow_charge_long_version", net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, .45f, 1f)
        held[player.uuid] = player.server.tickCount
    }
    private fun cancelBow(player: ServerPlayer) {
        held.remove(player.uuid)
        set(player, bow, "draw", 0.0)
        set(player, bow, "chorus", 0.0)
        set(player, bow, "afterglow", 0.0)
        set(player, bow, "display_draw", 0.0)
    }
    private fun tickBow(player: ServerPlayer) {
        if (!usable(player, bow)) { set(player, id("dawnbreaker/presentation"), "shot", 0.0); set(player, bow, "radiance", 0.0); if (player.uuid in held || resource(player, bow, "draw") > 0 || resource(player, bow, "afterglow") > 0) cancelBow(player); return }
        val visuals = if (radiant(player)) 1.0 else 0.0
        if (resource(player, bow, "radiance") != visuals) set(player, bow, "radiance", visuals)
        val last = held[player.uuid] ?: return
        if (player.server.tickCount - last <= 3) {
            val draw = (resource(player, bow, "draw") + 1).coerceAtMost(60.0)
            val chorus = floor(draw / 20.0)
            if (chorus > resource(player, bow, "chorus")) {
                sound(player.serverLevel(), player.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, .45f, .8f + chorus.toFloat() * .2f)
            }
            set(player, bow, "draw", draw); set(player, bow, "display_draw", draw); set(player, bow, "chorus", chorus)
        } else {
            held.remove(player.uuid)
            val chorus = resource(player, bow, "chorus").toInt()
            set(player, bow, "draw", 0.0); set(player, bow, "chorus", 0.0); set(player, bow, "afterglow", 6.0)
            if (chorus < 1) return
            val arrow = net.minecraft.world.entity.projectile.Arrow(player.serverLevel(), player,
                net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null)
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED
            arrow.setNoGravity(true)
            arrow.setBaseDamage(chorus * 10.0 / 3.0)
            arrow.customName = Component.literal("Dawnbreaker Light Arrow")
            arrow.isCustomNameVisible = false
            arrow.addTag("andromeda_dawn_light")
            arrow.addTag("andromeda_dawn_chorus_$chorus")
            arrow.shootFromRotation(player, player.xRot, player.yRot, 0f, 3f, 0f)
            if (!player.serverLevel().addFreshEntity(arrow)) return
            set(player, id("dawnbreaker/presentation"), "shot", 12.0)
            set(player, bow, "cooldown", 0.0)
            sound(player.serverLevel(), player.position(), net.minecraft.sounds.SoundEvents.ARROW_SHOOT, .65f, 1.3f)
            bowSound(player.serverLevel(), player.position(), "elemental_bow_holy_shoot_flyby_01", net.minecraft.sounds.SoundEvents.TRIDENT_THROW.value(), .65f, 1f)
            bowSound(player.serverLevel(), player.position(), "magic_bow_shoot_flyby_02", net.minecraft.sounds.SoundEvents.TRIDENT_THROW.value(), .8f, 1f)
            if (radiant(player)) {
                MrpgFxBridge.play(player.serverLevel(), player.eyePosition.add(player.lookAngle), EnhancedFxDefinitions.MrpgDefinition("mrpg_lib:preset/cast", .4f, .22f, 0xffd878, DawnbreakerPresentation.particles(3)), player, false)
                player.serverLevel().sendParticles(ParticleTypes.END_ROD, arrow.x, arrow.y, arrow.z, 3, .035, .035, .035, .01)
            }
            success(player, "Seraph’s Draw — ${chorus} Chorus released")
        }
    }

    private fun castBanish(player: ServerPlayer) {
        if (!usable(player, begone)) return
        set(player, begone, "cooldown", 0.0)
        val eye = player.eyePosition
        val end = eye.add(player.lookAngle.scale(32.0))
        // Ray-box targeting is narrow and stops at blocks; unrelated bystanders are never substituted.
        val hitBlock = player.serverLevel().clip(ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
        val limit = eye.distanceToSqr(hitBlock.location)
        val target = player.serverLevel().getEntitiesOfClass(LivingEntity::class.java, player.boundingBox.expandTowards(player.lookAngle.scale(32.0)).inflate(1.0)) { it !== player && it.isAlive && !it.isSpectator && !it.isVehicle }
            .mapNotNull { candidate -> candidate.boundingBox.inflate(.15).clip(eye, end).orElse(null)?.let { candidate to it } }
            .filter { eye.distanceToSqr(it.second) < limit && eye.distanceToSqr(it.second) <= 32.0 * 32.0 }
            .minByOrNull { eye.distanceToSqr(it.second) }?.first
        if (target == null) { say(player, "Look directly at a player or mob within 32 blocks."); return }
        var away = Vec3(target.x - player.x, 0.0, target.z - player.z)
        if (away.lengthSqr() < .0001) away = Vec3(player.lookAngle.x, 0.0, player.lookAngle.z)
        if (away.lengthSqr() < .0001) away = Vec3(0.0, 0.0, 1.0)
        val destination = target.position().add(away.normalize().scale(500.0))
        val level = player.serverLevel()
        // Exact displacement at the target's current height: no surface search, hazard test,
        // alternative columns, warning, pending request or asynchronous cancellation.
        val vehicle = target.vehicle
        target.stopRiding()
        if (!target.teleportTo(level, destination.x, destination.y, destination.z, mutableSetOf<RelativeMovement>(), target.yRot, target.xRot)) {
            if (vehicle != null && !vehicle.isRemoved) target.startRiding(vehicle, true)
            say(player, "Banishment failed."); return
        }
        target.deltaMovement = Vec3.ZERO
        target.fallDistance = 0f
        say(player, "Begone.")
        if (target is ServerPlayer) target.sendSystemMessage(Component.literal("You have been banished."))
        if (target is ServerPlayer && EnhancedFxConfig.enabled() && EnhancedFxConfig.sounds() &&
            Platform.canReceiveCosmetic(target, andromeda.origins.network.BanishmentHeartbeatPayload.TYPE.id()))
            Platform.sendCosmetic(target, andromeda.origins.network.BanishmentHeartbeatPayload.INSTANCE)
    }

    fun prepareLightArrowDamage(arrow: net.minecraft.world.entity.projectile.AbstractArrow) {
        if (arrow.level().isClientSide || "andromeda_dawn_light" !in arrow.tags) return
        val chorus = (1..3).firstOrNull { "andromeda_dawn_chorus_$it" in arrow.tags } ?: return
        val speed = arrow.deltaMovement.length()
        // Vanilla multiplies base damage by current speed and rounds up. Normalize at impact
        // so air drag cannot turn the promised 10/20/30 into lower damage at longer ranges.
        if (speed.isFinite() && speed > .0001) arrow.setBaseDamage((chorus * 10.0 - .00001) / speed)
    }
    fun lightArrowTrail(arrow: net.minecraft.world.entity.projectile.AbstractArrow) {
        if ("andromeda_dawn_light" !in arrow.tags) return
        val level = arrow.level() as? ServerLevel ?: return
        val caster = arrow.owner as? ServerPlayer ?: return
        if (!radiant(caster)) return
        val step = arrow.deltaMovement.scale(1.0 / 5)
        for (i in 0..4) {
            val at = arrow.position().subtract(step.scale(i.toDouble()))
            level.sendParticles(gold, at.x, at.y, at.z, 1, .015, .015, .015, 0.0)
        }
    }
    fun lightArrowImpact(arrow: net.minecraft.world.entity.projectile.AbstractArrow, at: Vec3) {
        if ("andromeda_dawn_light" !in arrow.tags) return
        val level = arrow.level() as? ServerLevel ?: return
        val caster = arrow.owner as? ServerPlayer ?: return
        bowSound(level, at, "elemental_bow_holy_shoot_impact_01", net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK, .55f, 1f)
        if (radiant(caster)) {
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 4, .12, .12, .12, .035)
            MrpgFxBridge.play(level, at, EnhancedFxDefinitions.MrpgDefinition("mrpg_lib:preset/impact", .55f, .3f, 0xffd878, DawnbreakerPresentation.particles(4)), caster, false)
        }
    }

    fun register() {
        Platform.join { handler, _, _ -> clear(handler.player) }
        Platform.disconnect { handler, _ -> clear(handler.player) }
        Platform.respawn { old, player, _ -> clear(old); clear(player) }
        Platform.death { entity, _, _ -> if (entity is ServerPlayer) clear(entity); true }
        Platform.serverStopped { server -> affected.clear(); statues.clear(); held.clear(); SovereignArts.stop(); DawnbreakerTransference.stop(server) }
        Platform.damage { entity, damage, _ ->
            val arrow = damage.directEntity
            val caster = damage.entity as? ServerPlayer
            if (arrow is net.minecraft.world.entity.projectile.Projectile && ("andromeda_dawn_light" in arrow.tags || "andromeda_stillwater_returned" in arrow.tags) &&
                caster != null && ally(caster, entity)) false else true
        }
        Platform.serverTick(::tick)
    }
    fun clear(player: ServerPlayer) {
        set(player, gaze, "recharge", 0.0)
        set(player, gaze, "charges", 2.0)
        set(player, aura, "cooldown", 0.0)
        set(player, begone, "cooldown", 0.0)
        set(player, aura, "active", 0.0)
        cancelBow(player)
        SovereignArts.clear(player)
        DawnbreakerTransference.clear(player)
        PowerContainer.of(player)?.let { c ->
            if (c.hasPower(id("dawnbreaker/secondary")) && !c.hasPower(id("gorgon/secondary"))) {
                val legacy = id("gorgon/helper/speedup")
                if (c.removePower(legacy, ResourceLocation.parse("minecraft:buff"))) {
                    if (!c.hasPower(id("gorgon/helper/speedup_speed"))) AttributePower.purge(player, id("gorgon/helper/speedup_speed"))
                    c.markDirty()
                }
            }
        }
        set(player, id("dawnbreaker/presentation"), "step", 0.0)
        set(player, id("dawnbreaker/presentation"), "shot", 0.0)
        if (!player.isAlive || player !in affected) remove(player, oppressed, source)
        if (!player.isAlive || player !in statues) remove(player, stone, source)
        if (!player.isAlive) affected.remove(player)
        // A caster's logout/death/change must not leave their own statues behind.
        for ((target, state) in statues.toMap()) {
            syncStatue(target, state); state.owners.remove(player.uuid)
            if (state.owners.isEmpty()) releaseStatue(target) else publishStatue(target, state)
        }
    }
    private fun remove(entity: LivingEntity, power: ResourceLocation, grant: ResourceLocation) {
        val c = PowerContainer.of(entity) ?: return
        val changed = c.removePower(power, grant)
        val speed = id(power.path + "_speed")
        val orphan = power == oppressed && entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
            ?.hasModifier(id("dawnbreaker_oppression")) == true
        if (!c.hasPower(speed) && (changed || orphan)) AttributePower.purge(entity, speed)
        if (changed) c.markDirty()
    }
    private fun releaseStatue(target: LivingEntity) {
        remove(target, petrify, cc); remove(target, stone, source)
        CrowdControlLifecycle.reconcile(target)
        if (target.isAlive && !target.isRemoved) shards(target)
        statues.remove(target)
    }
    private fun tick(server: MinecraftServer) {
        SovereignArts.tick(server)
        DawnbreakerTransference.tick(server)
        for ((target, state) in statues.toMap()) {
            syncStatue(target, state)
            state.owners.keys.removeIf { owner -> server.playerList.getPlayer(owner)?.let { usable(it, gaze) && it.level() === target.level() } != true }
            val owners = state.owners.keys
            val c = PowerContainer.of(target)
            if (owners.isEmpty() || !target.isAlive || target.isRemoved || c?.hasPower(petrify) != true) releaseStatue(target)
            else {
                publishStatue(target, state)
                if (owners.any { server.playerList.getPlayer(it)?.let { p -> owns(p, stoneAppearance) } == true }) {
                    val c = PowerContainer.of(target)
                    if (c != null) {
                        if (source !in c.sourcesOf(stone)) { c.addPower(stone, source); c.markDirty() }
                        set(target, stone, "gradual", if (owners.any { server.playerList.getPlayer(it)?.let { p -> owns(p, id("dawnbreaker/gradual_petrification")) } == true }) 1.0 else 0.0)
                    }
                } else remove(target, stone, source)
            }
        }
        val desired = mutableSetOf<LivingEntity>()
        for (caster in server.playerList.players) {
            set(caster, id("dawnbreaker/presentation"), "tier", DawnbreakerPresentation.current().ordinal.toDouble())
            set(caster, id("dawnbreaker/presentation"), "animation", if (DawnbreakerPresentation.settings().animatedWings.get()) 1.0 else 0.0)
            set(caster, id("dawnbreaker/presentation"), "hud", if (DawnbreakerPresentation.settings().guardHud.get()) 1.0 else 0.0)
            if (!owns(caster, id("dawnbreaker/measured_step"))) set(caster, id("dawnbreaker/presentation"), "step", 0.0)
            tickBow(caster)
            if (!usable(caster, aura)) {
                if (resource(caster, aura, "active") > 0) set(caster, aura, "active", 0.0)
                continue
            }
            val visuals = if (radiant(caster)) 1.0 else 0.0
            if (resource(caster, aura, "radiance") != visuals) set(caster, aura, "radiance", visuals)
            if (resource(caster, aura, "active") <= 0) continue
            val level = caster.serverLevel()
            if (server.tickCount % 10 == 0 && radiant(caster)) {
                MrpgFxBridge.play(level, caster.position().add(0.0, .15, 0.0), EnhancedFxDefinitions.MrpgDefinition("mrpg_lib:preset/aura", 1.1f, .12f, 0xffd878, 10), caster, false)
            }
            for (target in level.getEntitiesOfClass(LivingEntity::class.java, caster.boundingBox.inflate(10.0))) {
                if (!validTarget(caster, target) || target.distanceToSqr(caster) > 100 || !caster.hasLineOfSight(target)) continue
                val toward = caster.eyePosition.subtract(target.eyePosition).normalize()
                if (target.lookAngle.dot(toward) < cos(Math.toRadians(60.0))) continue
                desired.add(target)
                val c = PowerContainerAttachment.getOrCreate(target) ?: continue
                if (source !in c.sourcesOf(oppressed)) { c.addPower(oppressed, source); c.markDirty() }
                set(target, oppressed, "lease", 6.0)
            }
        }
        for (entity in affected - desired) remove(entity, oppressed, source)
        affected.clear(); affected.addAll(desired)
    }
    internal fun radiant(player: ServerPlayer) = owns(player, radiance) && EnhancedFxConfig.enabled() && EnhancedFxConfig.particles()
    private fun shards(entity: LivingEntity) {
        val level = entity.level() as? ServerLevel ?: return
        if (EnhancedFxConfig.enabled() && EnhancedFxConfig.particles()) {
            level.sendParticles(BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()), entity.x, entity.y + .35, entity.z, 3, .16, .2, .16, .01)
        }
    }
    private fun ring(entity: LivingEntity, level: ServerLevel, pos: Vec3, radius: Double, count: Int) {
        if (entity !is ServerPlayer || !radiant(entity)) return
        for (i in 0 until count) {
            val angle = i * Math.PI * 2 / count
            level.sendParticles(if (i % 2 == 0) gold else jade, pos.x + cos(angle) * radius, pos.y + .12, pos.z + sin(angle) * radius, 1, 0.0, .025, 0.0, 0.0)
        }
    }
    private fun bowSound(level: ServerLevel, pos: Vec3, path: String, fallback: net.minecraft.sounds.SoundEvent, volume: Float, pitch: Float) {
        val key = ResourceLocation.fromNamespaceAndPath("simplyswords", path)
        val registry = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
        sound(level, pos, if (registry.containsKey(key)) registry.get(key) ?: fallback else fallback, volume, pitch)
    }
    internal fun abilitySound(player: ServerPlayer, cue: net.minecraft.sounds.SoundEvent, volume: Float, pitch: Float) =
        sound(player.serverLevel(), player.position(), cue, volume, pitch)
    private fun sound(level: ServerLevel, pos: Vec3, sound: net.minecraft.sounds.SoundEvent, volume: Float, pitch: Float) {
        if (EnhancedFxConfig.enabled() && EnhancedFxConfig.sounds())
            level.playSound(null, pos.x, pos.y, pos.z, sound, net.minecraft.sounds.SoundSource.PLAYERS, volume * DawnbreakerPresentation.soundScale(), pitch)
    }
}
