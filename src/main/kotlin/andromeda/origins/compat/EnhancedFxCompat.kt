package andromeda.origins.compat

import andromeda.origins.compat.EnhancedFxConfig
import andromeda.origins.compat.EnhancedFxDefinitions
import andromeda.origins.compat.EnhancedFxDefinitions.EventDefinition
import andromeda.origins.platform.Platform
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import org.slf4j.Logger
import org.slf4j.LoggerFactory


 /**
 * Optional audiovisual compatibility for Spell Engine and More RPG Library.
 */
object EnhancedFxCompat {
    val LOGGER: Logger = LoggerFactory.getLogger("Andromeda Origins/Enhanced FX")
    val WARNED: MutableSet<String> = ConcurrentHashMap.newKeySet<String>()

    const val SPELL_ENGINE_ID: String = "spell_engine"
    const val MORE_RPG_ID: String = "more_rpg_classes"

    var spellEngineLoaded: Boolean = false
    var moreRpgLoaded: Boolean = false
    var initialized: Boolean = false

    fun initialize() {
        if (EnhancedFxCompat.initialized) {
            return
        }
        EnhancedFxCompat.initialized = true
        EnhancedFxConfig.load()
        EnhancedFxDefinitions.register()
        EnhancedFxCompat.spellEngineLoaded = Platform.isModLoaded(EnhancedFxCompat.SPELL_ENGINE_ID)
        EnhancedFxCompat.moreRpgLoaded = (Platform.isModLoaded("mrpg_lib") || Platform.isModLoaded(EnhancedFxCompat.MORE_RPG_ID))
    }

    fun active(): Boolean {
        EnhancedFxCompat.initialize()
        return EnhancedFxConfig.enabled()
    }

    fun statusLine(): String {
        EnhancedFxCompat.initialize()
        return ("Enhanced FX=" + EnhancedFxConfig.enabled()
        + ", particles=" + EnhancedFxConfig.particles()
        + ", sounds=" + EnhancedFxConfig.sounds()
        + ", Spell Engine=" + EnhancedFxCompat.spellEngineLoaded
        + ", More RPG Library=" + EnhancedFxCompat.moreRpgLoaded
        + ", definitions=" + EnhancedFxDefinitions.count())
    }

     /** Plays an Enhanced FX event at the source entity.  */
    fun play(entity: Entity, event: String): Boolean {
        return EnhancedFxCompat.playInternal(entity, event, null)
    }

    fun playAt(entity: Entity, event: String, position: Vec3?): Boolean {
        return EnhancedFxCompat.playInternal(entity, event, position)
    }

    private fun playInternal(entity: Entity, event: String, position: Vec3?): Boolean {
        EnhancedFxCompat.initialize()
        if ((entity.level() !is ServerLevel) || !EnhancedFxConfig.enabled()) {
            return false
        }

        if (!OriginFxStyle.event(entity, event)) return true
        val definition: EventDefinition? = EnhancedFxDefinitions.get(event)
        if (definition == null) {
            EnhancedFxCompat.warnOnce("event:" + event, "Enhanced FX event '{}' is not defined; bundled fallback FX will continue.", event)
            return false
        }

        if (EnhancedFxConfig.particles()) {
            val specs = definition.particles.filter { requirementPresent(it.requiresMod) }.toMutableList()
            val minimal = OriginFxStyle.quality() == OriginFxStyle.Quality.MINIMAL
            // Texture-based library effects are the normal presentation, not a suppressed fallback.
            val textured = !minimal && ((spellEngineLoaded && ParticleBridge.spawn(entity, position, specs)) ||
                simpleLibraryParticles(entity, position, specs))
            val preset = !minimal && !textured && definition.mrpg?.let {
                MrpgFxBridge.play(entity.level() as ServerLevel, position ?: entity.position(), it, entity, position == null)
            } == true
            // Keep the clean geometry as an occasional accent; sustained library effects stand alone.
            if ((!textured && !preset) || (!OriginFxStyle.sustained(event) && event !in setOf("fenrkin.on_the_hunt", "fenrkin.mark")))
                OriginContourBridge.play(entity, event, position, definition)
        }

        if (EnhancedFxConfig.sounds()) {
            val x: Double = if (position != null) position.x else entity.getX()
            val y: Double = if (position != null) position.y else entity.getY()
            val z: Double = if (position != null) position.z else entity.getZ()
            val available = definition.sounds.firstOrNull { sound ->
                requirementPresent(sound.requiresMod) && effectId(sound.id)?.let {
                    sound.resourcePack || BuiltInRegistries.SOUND_EVENT.containsKey(it)
                } == true
            }
            // Native contours must not depend on an optional sound library being installed.
            val fallback = when (event.substringBefore('.')) {
                "arachne" -> "minecraft:block.azalea_leaves.step"
                "faerie", "gorgon" -> "minecraft:block.amethyst_block.chime"
                "fenrkin" -> null
                "humanity" -> "minecraft:block.beacon.power_select"
                "lichling" -> "minecraft:block.soul_sand.step"
                "manticore" -> "minecraft:entity.phantom.flap"
                "nereid", "selkie", "siren" -> "minecraft:entity.player.splash"
                "satyr" -> "minecraft:block.azalea.step"
                "veilborn" -> "minecraft:block.amethyst_block.resonate"
                "wyverian" -> "minecraft:item.firecharge.use"
                else -> null
            }
            if (available != null) playSound(entity, x, y, z, available)
            else if (fallback != null && definition.sounds.isNotEmpty() && !OriginFxStyle.sustained(event)) playSound(entity, x, y, z,
                EnhancedFxDefinitions.SoundDefinition(fallback, .45f, 1f, "", false))
        }
        return true
    }

    private fun simpleLibraryParticles(entity: Entity, position: Vec3?, specs: List<EnhancedFxDefinitions.ParticleDefinition>): Boolean {
        val level = entity.level() as? ServerLevel ?: return false
        val origin = position ?: entity.position()
        var sent = false
        for (spec in specs.take(OriginFxStyle.layers())) {
            val id = effectId(spec.id) ?: continue
            val type = BuiltInRegistries.PARTICLE_TYPE.get(id) as? SimpleParticleType ?: continue
            val viewers = level.players().filter { it.distanceToSqr(origin) <= 32.0 * 32.0 &&
                (id.namespace == "minecraft" || Platform.canReceiveCosmetic(it, ResourceLocation.parse("mrpg_lib:fx_play"))) }
            if (viewers.isEmpty()) continue
            val count = OriginFxStyle.particles(entity, spec.count.toInt()) ?: continue
            val ground = spec.preset in setOf("ground", "placed")
            val radius = if (ground) .08 else .22
            val y = origin.y + if (ground) .08 else entity.bbHeight * .5
            for (viewer in viewers) level.sendParticles(viewer, type, false, origin.x, y, origin.z, count, radius, if (ground) .01 else .32, radius, spec.speed.toDouble().coerceIn(0.0, .15))
            sent = true
        }
        return sent
    }

    private fun requirementPresent(modId: String?): Boolean {
        if (modId == null || modId.isBlank()) {
            return true
        }
        if (MORE_RPG_ID == modId || "mrpg_lib" == modId) {
            return EnhancedFxCompat.moreRpgLoaded
        }
        if (SPELL_ENGINE_ID == modId) {
            return EnhancedFxCompat.spellEngineLoaded
        }
        return Platform.isModLoaded(modId)
    }

    /** Keep legacy FX definitions usable with the current More RPG Library namespace. */
    private fun effectId(value: String): ResourceLocation? {
        val id = ResourceLocation.tryParse(value) ?: return null
        return if (id.namespace == MORE_RPG_ID && Platform.isModLoaded("mrpg_lib")) {
            ResourceLocation.fromNamespaceAndPath("mrpg_lib", id.path)
        } else id
    }

    private fun playSound(entity: Entity, x: Double, y: Double, z: Double, definition: EnhancedFxDefinitions.SoundDefinition) {
        val id: ResourceLocation? = effectId(definition.id)
        if (id == null || !definition.volume.isFinite() || !definition.pitch.isFinite()) {
            EnhancedFxCompat.warnOnce("sound:" + definition.id, "Invalid enhanced sound '{}'; skipping optional sound layer.", definition.id)
            return
        }
        if (!OriginFxStyle.sound(entity, id.toString() + ":" + definition.pitch)) return
        val sound = (if (BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
            BuiltInRegistries.SOUND_EVENT.get(id)
        } else if (definition.resourcePack) {
            // Direct sound events let clients provide sounds.json and audio without a Java registry entry.
            SoundEvent.createVariableRangeEvent(id)
        } else {
            EnhancedFxCompat.warnOnce("sound:" + definition.id, "Enhanced FX sound '{}' is not registered; skipping optional sound layer.", definition.id)
            return
        }) ?: return
        entity.level().playSound(
        null,
        x, y, z,
        sound,
        SoundSource.PLAYERS,
        OriginFxStyle.volume(definition.volume),
        definition.pitch.coerceIn(0.5f, 2f)
        )
    }

    private fun warnOnce(key: String, message: String, vararg args: Any?) {
        if (EnhancedFxCompat.WARNED.add(key)) EnhancedFxCompat.LOGGER.warn(message, *args)
    }

     /** Reflection-only adapter so Spell Engine is never a JVM linkage requirement.  */
    private object ParticleBridge {
        var attempted: Boolean = false
        var available: Boolean = false
        lateinit var easingClass: Class<out Enum<*>>
        lateinit var renderClass: Class<out Enum<*>>
        lateinit var builderRender: Method
        lateinit var motionClass: Class<out Enum<*>>
        lateinit var builderOf: Method
        lateinit var builderScale: Method
        lateinit var builderScaleVariance: Method
        lateinit var builderScaleTo: Method
        lateinit var builderColor: Method
        lateinit var builderColorVariance: Method
        lateinit var builderOpacity: Method
        lateinit var builderFadeOut: Method
        lateinit var builderFadeInOut: Method
        lateinit var builderPlaybackSpeed: Method
        lateinit var builderLifetimeVariance: Method
        lateinit var builderGlow: Method
        lateinit var builderMotion: Method
        lateinit var builderGravity: Method
        lateinit var builderDrag: Method
        lateinit var builderCollides: Method
        lateinit var builderAttached: Method
        lateinit var builderAttachedToGround: Method
        lateinit var builderBatch: Method
        lateinit var sendBatches: Method
        lateinit var sendBatchesAt: Method
        lateinit var batchPreTravel: Method
        lateinit var batchesImpact: Method
        lateinit var batchesCasting: Method
        lateinit var batchesTravel: Method
        lateinit var batchesCloud: Method
        lateinit var batchesShockwave: Method
        lateinit var batchesGround: Method
        lateinit var batchesPlaced: Method
        lateinit var batchesCone: Method
        lateinit var batchesPopUp: Method
        lateinit var batchesHelix: Method

        fun init(): Boolean {
            if (ParticleBridge.attempted) {
                return ParticleBridge.available
            }
            ParticleBridge.attempted = true
            try {
                val builderClass: Class<*> = Class.forName("net.spell_engine.api.spell.fx.ParticleGroupBuilder")
                val batchesClass: Class<*> = Class.forName("net.spell_engine.api.spell.fx.ParticleGroupBuilder\$Batches")
                val helperClass: Class<*> = Class.forName("net.spell_engine.fx.ParticleHelper")
                ParticleBridge.easingClass = Class.forName("net.spell_engine.api.spell.fx.Easing") as Class<out Enum<*>>
                ParticleBridge.renderClass = Class.forName("net.spell_engine.api.spell.fx.ParticleGroup\$Render") as Class<out Enum<*>>
                ParticleBridge.builderRender = builderClass.getMethod("render", ParticleBridge.renderClass)
                ParticleBridge.motionClass = Class.forName("net.spell_engine.api.spell.fx.ParticleGroup\$Motion") as Class<out Enum<*>>

                ParticleBridge.builderOf = builderClass.getMethod("of", ResourceLocation::class.java)
                ParticleBridge.builderScale = builderClass.getMethod("scale", Float::class.javaPrimitiveType)
                ParticleBridge.builderScaleVariance = builderClass.getMethod("scale", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                ParticleBridge.builderScaleTo = builderClass.getMethod("scaleTo", Float::class.javaPrimitiveType, ParticleBridge.easingClass)
                ParticleBridge.builderColor = builderClass.getMethod("color", Long::class.javaPrimitiveType)
                ParticleBridge.builderColorVariance = builderClass.getMethod("colorVariance", Float::class.javaPrimitiveType)
                ParticleBridge.builderOpacity = builderClass.getMethod("opacity", Float::class.javaPrimitiveType)
                ParticleBridge.builderFadeOut = builderClass.getMethod("fadeOut", Float::class.javaPrimitiveType, ParticleBridge.easingClass)
                ParticleBridge.builderFadeInOut = builderClass.getMethod("fadeInOut", Float::class.javaPrimitiveType, ParticleBridge.easingClass)
                ParticleBridge.builderPlaybackSpeed = builderClass.getMethod("playbackSpeed", Float::class.javaPrimitiveType)
                ParticleBridge.builderLifetimeVariance = builderClass.getMethod("lifetimeVariance", Float::class.javaPrimitiveType)
                ParticleBridge.builderGlow = builderClass.getMethod("glow", Boolean::class.javaPrimitiveType)
                ParticleBridge.builderMotion = builderClass.getMethod("motion", ParticleBridge.motionClass)
                ParticleBridge.builderGravity = builderClass.getMethod("gravity", Float::class.javaPrimitiveType)
                ParticleBridge.builderDrag = builderClass.getMethod("drag", Float::class.javaPrimitiveType)
                ParticleBridge.builderCollides = builderClass.getMethod("collides")
                ParticleBridge.builderAttached = builderClass.getMethod("attached")
                ParticleBridge.builderAttachedToGround = builderClass.getMethod("attachedToGround")
                ParticleBridge.builderBatch = builderClass.getMethod("batch", Consumer::class.java)
                ParticleBridge.sendBatches = helperClass.getMethod("sendBatches", Entity::class.java, MutableList::class.java)
                ParticleBridge.sendBatchesAt = helperClass.getMethod("sendBatches", Vec3::class.java, LivingEntity::class.java, MutableList::class.java)

                ParticleBridge.batchPreTravel = Class.forName("net.spell_engine.api.spell.fx.ParticleGroup\$Batch")
                    .getMethod("preTravel", Float::class.javaPrimitiveType)
                ParticleBridge.batchesImpact = batchesClass.getMethod("impact", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                ParticleBridge.batchesCasting = batchesClass.getMethod("casting", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                ParticleBridge.batchesTravel = batchesClass.getMethod("travel", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                ParticleBridge.batchesCloud = batchesClass.getMethod("cloud", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                ParticleBridge.batchesShockwave = batchesClass.getMethod("shockwave", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                ParticleBridge.batchesGround = batchesClass.getMethod("ground", Float::class.javaPrimitiveType)
                ParticleBridge.batchesPlaced = batchesClass.getMethod("placed", Float::class.javaPrimitiveType)
                ParticleBridge.batchesCone = batchesClass.getMethod("cone", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                ParticleBridge.batchesPopUp = batchesClass.getMethod("popUp")
                ParticleBridge.batchesHelix = batchesClass.getMethod("helix", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                ParticleBridge.available = true
            }catch (exception: ReflectiveOperationException) {
                ParticleBridge.available = false
                EnhancedFxCompat.warnOnce("spell_engine_api",
                "Spell Engine is present but its particle API did not match Andromeda's optional bridge; enhanced particles are disabled for this session.",
                exception)
            }
            return ParticleBridge.available
        }

        fun spawn(entity: Entity, position: Vec3?, specs: MutableList<EnhancedFxDefinitions.ParticleDefinition>): Boolean {
            if (!ParticleBridge.init()) return false
            try {
                val effects: MutableList<Any> = ArrayList<Any>(specs.size)
                for (original in specs.take(OriginFxStyle.layers())) {
                    val count = OriginFxStyle.particles(entity, original.count.toInt()) ?: continue
                    val spec = original.copy(count = count.toFloat(), scale = original.scale.coerceIn(.05f,OriginFxStyle.particleScaleLimit()), scaleTo = original.scaleTo.coerceIn(.05f,OriginFxStyle.particleScaleLimit()), scaleVariance = original.scaleVariance.coerceIn(0f,.15f), opacity = original.opacity.coerceIn(0f,.85f))
                    val id: ResourceLocation? = effectId(spec.id)
                    if (id == null || !BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) {
                        EnhancedFxCompat.warnOnce("particle:" + spec.id,
                        "Enhanced FX particle '{}' is not registered; skipping optional particle layer.", spec.id)
                        continue
                    }
                    val builder = requireNotNull(ParticleBridge.builderOf.invoke(null, id))
                    ParticleBridge.applyAppearance(builder, spec)
                    val effect = requireNotNull(ParticleBridge.builderBatch.invoke(builder, ParticleBridge.batchPreset(spec)))
                    effects.add(effect)
                }
                if (!effects.isEmpty()) {
                    val living = entity as? LivingEntity
                    if (position != null && living != null) {
                        ParticleBridge.sendBatchesAt.invoke(null, position, entity, effects)
                    } else {
                        ParticleBridge.sendBatches.invoke(null, entity, effects)
                    }
                    return true
                }
            }catch (exception: ReflectiveOperationException) {
                ParticleBridge.available = false
                EnhancedFxCompat.warnOnce("spell_engine_runtime",
                "Spell Engine particle invocation failed; enhanced particles are disabled for the rest of this session.",
                exception)
            }
            return false
        }

        @Throws(ReflectiveOperationException::class) fun applyAppearance(builder: Any, spec: EnhancedFxDefinitions.ParticleDefinition) {
            var builder: Any? = builder
            if (spec.scaleVariance > 0f) {
                builder = ParticleBridge.builderScaleVariance.invoke(builder, spec.scale, spec.scaleVariance)
            } else if (spec.scale > 0f && spec.scale != 1f) {
                builder = ParticleBridge.builderScale.invoke(builder, spec.scale)
            }
            if (spec.scaleTo != 1f) {
                builder = ParticleBridge.builderScaleTo.invoke(builder, spec.scaleTo, ParticleBridge.enumValue(ParticleBridge.easingClass, "LINEAR"))
            }
            if (!spec.color.isBlank()) {
                val color: Long = ParticleBridge.parseColor(spec.color)
                if (color != -1L) {
                    builder = ParticleBridge.builderColor.invoke(builder, color)
                }
            }
            if (spec.colorVariance > 0f) {
                builder = ParticleBridge.builderColorVariance.invoke(builder, spec.colorVariance)
            }
            if (spec.opacity != 1f) {
                builder = ParticleBridge.builderOpacity.invoke(builder, spec.opacity)
            }
            if (spec.fadeOutHold >= 0f) {
                builder = ParticleBridge.builderFadeOut.invoke(builder, spec.fadeOutHold, ParticleBridge.enumValue(ParticleBridge.easingClass, "LINEAR"))
            }
            if (spec.fadeInOutHold >= 0f) {
                builder = ParticleBridge.builderFadeInOut.invoke(builder, spec.fadeInOutHold, ParticleBridge.enumValue(ParticleBridge.easingClass, "LINEAR"))
            }
            if (spec.playbackSpeed != 1f) {
                builder = ParticleBridge.builderPlaybackSpeed.invoke(builder, spec.playbackSpeed)
            }
            if (spec.lifetimeVariance > 0f) {
                builder = ParticleBridge.builderLifetimeVariance.invoke(builder, spec.lifetimeVariance)
            }
            if (spec.render.isNotBlank()) {
                ParticleBridge.enumValue(ParticleBridge.renderClass, spec.render)?.let { ParticleBridge.builderRender.invoke(builder, it) }
            }
            if (spec.glow) {
                builder = ParticleBridge.builderGlow.invoke(builder, true)
            }
            if (!spec.motion.isBlank()) {
                val motion: Any? = ParticleBridge.enumValue(ParticleBridge.motionClass, spec.motion)
                if (motion != null) {
                    builder = ParticleBridge.builderMotion.invoke(builder, motion)
                }
            }
            if (spec.gravity != null) {
                builder = ParticleBridge.builderGravity.invoke(builder, spec.gravity)
            }
            if (spec.drag != null) {
                builder = ParticleBridge.builderDrag.invoke(builder, spec.drag)
            }
            if (spec.collides) {
                builder = ParticleBridge.builderCollides.invoke(builder)
            }
            if (spec.attachedToGround) {
                builder = ParticleBridge.builderAttachedToGround.invoke(builder)
            } else if (spec.attached) {
                builder = ParticleBridge.builderAttached.invoke(builder)
            }
        }

        @Throws(ReflectiveOperationException::class) fun batchPreset(spec: EnhancedFxDefinitions.ParticleDefinition): Any? {
            val preset: String = spec.preset.lowercase()
            val base = when (preset){"casting" -> ParticleBridge.batchesCasting.invoke(null, spec.count, spec.speed)
                "travel" -> ParticleBridge.batchesTravel.invoke(null, spec.count, spec.speed)
                "cloud" -> ParticleBridge.batchesCloud.invoke(null, spec.count, spec.speed)
                "shockwave" -> ParticleBridge.batchesShockwave.invoke(null, spec.count, spec.speed, spec.preTravel)
                "ground" -> ParticleBridge.batchesGround.invoke(null, spec.count)
                "placed" -> ParticleBridge.batchesPlaced.invoke(null, spec.count)
                "cone" -> ParticleBridge.batchesCone.invoke(null, spec.count, spec.speed, spec.angle)
                "helix" -> ParticleBridge.batchesHelix.invoke(null, spec.count, spec.speed, spec.degreesPerTick, spec.offset)
                "pop_up", "popup" -> ParticleBridge.batchesPopUp.invoke(null)
                else -> ParticleBridge.batchesImpact.invoke(null, spec.count, spec.speed)}
            if (spec.preTravel <= 0f || preset == "shockwave") return base
            @Suppress("UNCHECKED_CAST")
            val consumer = base as Consumer<Any>
            return Consumer<Any> { batch ->
                consumer.accept(batch)
                ParticleBridge.batchPreTravel.invoke(batch, spec.preTravel.coerceIn(0f, 3f))
            }
        }

        fun parseColor(input: String): Long {
            var s: String = input.trim {it <= ' '}
            if (s.startsWith("#")) {
                s = s.substring(1)
            }
            try {
                if (s.length == 6) {
                    // Spell Engine packs colors as RGBA, not ARGB. Append opaque alpha.
                    return (s + "FF").toLong(16)
                }
                if (s.length == 8) {
                    return s.toLong(16)
                }
            }catch (ignored: NumberFormatException) {
            }
            return -1L
        }

        fun enumValue(enumClass: Class<out Enum<*>>?, name: String?): Any? {
            if (enumClass == null || name == null || name.isBlank()) {
                return null
            }
            try {
                return enumClass.enumConstants.firstOrNull { it.name == name.trim().uppercase() }
            }catch (ignored: IllegalArgumentException) {
                return null
            }
        }
    }
}
