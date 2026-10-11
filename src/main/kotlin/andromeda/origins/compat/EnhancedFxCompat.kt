package andromeda.origins.compat

import andromeda.origins.compat.EnhancedFxConfig
import java.util.concurrent.ConcurrentHashMap
import andromeda.origins.compat.EnhancedFxDefinitions
import andromeda.origins.compat.EnhancedFxDefinitions.EventDefinition


 /**
 * Optional audiovisual compatibility for Spell Engine and More RPG Library.
 */
object EnhancedFxCompat {
    val LOGGER: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("Andromeda Origins/Enhanced FX")
    val WARNED: kotlin.collections.MutableSet<kotlin.String> = ConcurrentHashMap.newKeySet<kotlin.String>()

    const val SPELL_ENGINE_ID: kotlin.String = "spell_engine"
    const val MORE_RPG_ID: kotlin.String = "more_rpg_classes"

    var spellEngineLoaded: kotlin.Boolean = false
    var moreRpgLoaded: kotlin.Boolean = false
    var initialized: kotlin.Boolean = false

    fun initialize() {
        if (EnhancedFxCompat.initialized) {
            return
        }
        EnhancedFxCompat.initialized = true
        EnhancedFxConfig.load()
        EnhancedFxDefinitions.register()
        EnhancedFxCompat.spellEngineLoaded = andromeda.origins.platform.Platform.isModLoaded(EnhancedFxCompat.SPELL_ENGINE_ID)
        EnhancedFxCompat.moreRpgLoaded = andromeda.origins.platform.Platform.isModLoaded(EnhancedFxCompat.MORE_RPG_ID)
    }

    fun active(): kotlin.Boolean {
        EnhancedFxCompat.initialize()
        return EnhancedFxConfig.enabled() && EnhancedFxCompat.spellEngineLoaded
    }

    fun statusLine(): kotlin.String {
        EnhancedFxCompat.initialize()
        return ("Enhanced FX=" + EnhancedFxConfig.enabled()
        + ", particles=" + EnhancedFxConfig.particles()
        + ", sounds=" + EnhancedFxConfig.sounds()
        + ", Spell Engine=" + EnhancedFxCompat.spellEngineLoaded
        + ", More RPG Library=" + EnhancedFxCompat.moreRpgLoaded
        + ", definitions=" + EnhancedFxDefinitions.count())
    }

     /** Plays an Enhanced FX event at the source entity.  */
    fun play(entity: net.minecraft.world.entity.Entity, event: kotlin.String): kotlin.Boolean {
        return EnhancedFxCompat.playInternal(entity, event, null)
    }

    fun playAt(entity: net.minecraft.world.entity.Entity, event: kotlin.String, position: net.minecraft.world.phys.Vec3?): kotlin.Boolean {
        return EnhancedFxCompat.playInternal(entity, event, position)
    }

    private fun playInternal(entity: net.minecraft.world.entity.Entity, event: kotlin.String, position: net.minecraft.world.phys.Vec3?): kotlin.Boolean {
        EnhancedFxCompat.initialize()
        if ((entity.level() !is net.minecraft.server.level.ServerLevel) || !EnhancedFxConfig.enabled() || !EnhancedFxCompat.spellEngineLoaded) {
            return false
        }

        val definition: EventDefinition? = EnhancedFxDefinitions.get(event)
        if (definition == null) {
            EnhancedFxCompat.warnOnce("event:" + event, "Enhanced FX event '{}' is not defined; bundled fallback FX will continue.", event)
            return false
        }

        if (EnhancedFxConfig.particles()) {
            val particles: kotlin.collections.MutableList<EnhancedFxDefinitions.ParticleDefinition> = definition.particles.stream()
            .filter { p: EnhancedFxDefinitions.ParticleDefinition -> EnhancedFxCompat.requirementPresent(p.requiresMod)}
            .toList()
            if (!particles.isEmpty()) {
                ParticleBridge.spawn(entity, position, particles)
            }
        }

        if (EnhancedFxConfig.sounds()) {
            val x: kotlin.Double = if (position != null) position.x else entity.getX()
            val y: kotlin.Double = if (position != null) position.y else entity.getY()
            val z: kotlin.Double = if (position != null) position.z else entity.getZ()
            for (sound in definition.sounds) {
                if (EnhancedFxCompat.requirementPresent(sound.requiresMod)) {
                    EnhancedFxCompat.playSound(entity, x, y, z, sound)
                }
            }
        }
        return true
    }

    private fun requirementPresent(modId: kotlin.String?): kotlin.Boolean {
        if (modId == null || modId.isBlank()) {
            return true
        }
        if (MORE_RPG_ID == modId) {
            return EnhancedFxCompat.moreRpgLoaded
        }
        if (SPELL_ENGINE_ID == modId) {
            return EnhancedFxCompat.spellEngineLoaded
        }
        return andromeda.origins.platform.Platform.isModLoaded(modId)
    }

    private fun playSound(entity: net.minecraft.world.entity.Entity, x: kotlin.Double, y: kotlin.Double, z: kotlin.Double, definition: EnhancedFxDefinitions.SoundDefinition) {
        val id: net.minecraft.resources.ResourceLocation? = net.minecraft.resources.ResourceLocation.tryParse(definition.id)
        if (id == null || !net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
            EnhancedFxCompat.warnOnce("sound:" + definition.id, "Enhanced FX sound '{}' is not registered; skipping optional sound layer.", definition.id)
            return
        }
        val sound: net.minecraft.sounds.SoundEvent? = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id)
        if (sound == null) {
            return
        }
        entity.level().playSound(
        null,
        x, y, z,
        sound,
        net.minecraft.sounds.SoundSource.PLAYERS,
        definition.volume,
        definition.pitch
        )
    }

    private fun warnOnce(key: kotlin.String, message: kotlin.String, vararg args: kotlin.Any?) {
        if (EnhancedFxCompat.WARNED.add(key)) EnhancedFxCompat.LOGGER.warn(message, *args)
    }

     /** Reflection-only adapter so Spell Engine is never a JVM linkage requirement.  */
    private object ParticleBridge {
        var attempted: kotlin.Boolean = false
        var available: kotlin.Boolean = false
        lateinit var easingClass: java.lang.Class<out kotlin.Enum<*>>
        lateinit var motionClass: java.lang.Class<out kotlin.Enum<*>>
        lateinit var builderOf: java.lang.reflect.Method
        lateinit var builderScale: java.lang.reflect.Method
        lateinit var builderScaleVariance: java.lang.reflect.Method
        lateinit var builderScaleTo: java.lang.reflect.Method
        lateinit var builderColor: java.lang.reflect.Method
        lateinit var builderColorVariance: java.lang.reflect.Method
        lateinit var builderOpacity: java.lang.reflect.Method
        lateinit var builderFadeOut: java.lang.reflect.Method
        lateinit var builderFadeInOut: java.lang.reflect.Method
        lateinit var builderPlaybackSpeed: java.lang.reflect.Method
        lateinit var builderLifetimeVariance: java.lang.reflect.Method
        lateinit var builderGlow: java.lang.reflect.Method
        lateinit var builderMotion: java.lang.reflect.Method
        lateinit var builderGravity: java.lang.reflect.Method
        lateinit var builderDrag: java.lang.reflect.Method
        lateinit var builderCollides: java.lang.reflect.Method
        lateinit var builderAttached: java.lang.reflect.Method
        lateinit var builderAttachedToGround: java.lang.reflect.Method
        lateinit var builderBatch: java.lang.reflect.Method
        lateinit var sendBatches: java.lang.reflect.Method
        lateinit var sendBatchesAt: java.lang.reflect.Method
        lateinit var batchesImpact: java.lang.reflect.Method
        lateinit var batchesCasting: java.lang.reflect.Method
        lateinit var batchesTravel: java.lang.reflect.Method
        lateinit var batchesCloud: java.lang.reflect.Method
        lateinit var batchesShockwave: java.lang.reflect.Method
        lateinit var batchesGround: java.lang.reflect.Method
        lateinit var batchesPlaced: java.lang.reflect.Method
        lateinit var batchesCone: java.lang.reflect.Method
        lateinit var batchesPopUp: java.lang.reflect.Method
        lateinit var batchesHelix: java.lang.reflect.Method

        fun init(): kotlin.Boolean {
            if (ParticleBridge.attempted) {
                return ParticleBridge.available
            }
            ParticleBridge.attempted = true
            try {
                val builderClass: java.lang.Class<*> = java.lang.Class.forName("net.spell_engine.api.spell.fx.ParticleGroupBuilder")
                val batchesClass: java.lang.Class<*> = java.lang.Class.forName("net.spell_engine.api.spell.fx.ParticleGroupBuilder\$Batches")
                val helperClass: java.lang.Class<*> = java.lang.Class.forName("net.spell_engine.fx.ParticleHelper")
                ParticleBridge.easingClass = java.lang.Class.forName("net.spell_engine.api.spell.fx.Easing") as java.lang.Class<out kotlin.Enum<*>>
                ParticleBridge.motionClass = java.lang.Class.forName("net.spell_engine.api.spell.fx.ParticleGroup\$Motion") as java.lang.Class<out kotlin.Enum<*>>

                ParticleBridge.builderOf = builderClass.getMethod("of", net.minecraft.resources.ResourceLocation::class.java)
                ParticleBridge.builderScale = builderClass.getMethod("scale", kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.builderScaleVariance = builderClass.getMethod("scale", kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.builderScaleTo = builderClass.getMethod("scaleTo", kotlin.Float::class.javaPrimitiveType, ParticleBridge.easingClass)
                ParticleBridge.builderColor = builderClass.getMethod("color", kotlin.Long::class.javaPrimitiveType)
                ParticleBridge.builderColorVariance = builderClass.getMethod("colorVariance", kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.builderOpacity = builderClass.getMethod("opacity", kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.builderFadeOut = builderClass.getMethod("fadeOut", kotlin.Float::class.javaPrimitiveType, ParticleBridge.easingClass)
                ParticleBridge.builderFadeInOut = builderClass.getMethod("fadeInOut", kotlin.Float::class.javaPrimitiveType, ParticleBridge.easingClass)
                ParticleBridge.builderPlaybackSpeed = builderClass.getMethod("playbackSpeed", kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.builderLifetimeVariance = builderClass.getMethod("lifetimeVariance", kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.builderGlow = builderClass.getMethod("glow", kotlin.Boolean::class.javaPrimitiveType)
                ParticleBridge.builderMotion = builderClass.getMethod("motion", ParticleBridge.motionClass)
                ParticleBridge.builderGravity = builderClass.getMethod("gravity", kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.builderDrag = builderClass.getMethod("drag", kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.builderCollides = builderClass.getMethod("collides")
                ParticleBridge.builderAttached = builderClass.getMethod("attached")
                ParticleBridge.builderAttachedToGround = builderClass.getMethod("attachedToGround")
                ParticleBridge.builderBatch = builderClass.getMethod("batch", java.util.function.Consumer::class.java)
                ParticleBridge.sendBatches = helperClass.getMethod("sendBatches", net.minecraft.world.entity.Entity::class.java, kotlin.collections.MutableList::class.java)
                ParticleBridge.sendBatchesAt = helperClass.getMethod("sendBatches", net.minecraft.world.phys.Vec3::class.java, net.minecraft.world.entity.LivingEntity::class.java, kotlin.collections.MutableList::class.java)

                ParticleBridge.batchesImpact = batchesClass.getMethod("impact", kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.batchesCasting = batchesClass.getMethod("casting", kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.batchesTravel = batchesClass.getMethod("travel", kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.batchesCloud = batchesClass.getMethod("cloud", kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.batchesShockwave = batchesClass.getMethod("shockwave", kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.batchesGround = batchesClass.getMethod("ground", kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.batchesPlaced = batchesClass.getMethod("placed", kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.batchesCone = batchesClass.getMethod("cone", kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.batchesPopUp = batchesClass.getMethod("popUp")
                ParticleBridge.batchesHelix = batchesClass.getMethod("helix", kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType, kotlin.Float::class.javaPrimitiveType)
                ParticleBridge.available = true
            }catch (exception: java.lang.ReflectiveOperationException) {
                ParticleBridge.available = false
                EnhancedFxCompat.warnOnce("spell_engine_api",
                "Spell Engine is present but its particle API did not match Andromeda's optional bridge; enhanced particles are disabled for this session.",
                exception)
            }
            return ParticleBridge.available
        }

        fun spawn(entity: net.minecraft.world.entity.Entity, position: net.minecraft.world.phys.Vec3?, specs: kotlin.collections.MutableList<EnhancedFxDefinitions.ParticleDefinition>) {
            if (!ParticleBridge.init()) {
                return
            }
            try {
                val effects: kotlin.collections.MutableList<kotlin.Any> = java.util.ArrayList<kotlin.Any>(specs.size)
                for (spec in specs) {
                    val id: net.minecraft.resources.ResourceLocation? = net.minecraft.resources.ResourceLocation.tryParse(spec.id)
                    if (id == null || !net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) {
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
                    val living = entity as? net.minecraft.world.entity.LivingEntity
                    if (position != null && living != null) {
                        ParticleBridge.sendBatchesAt.invoke(null, position, entity, effects)
                    } else {
                        ParticleBridge.sendBatches.invoke(null, entity, effects)
                    }
                }
            }catch (exception: java.lang.ReflectiveOperationException) {
                ParticleBridge.available = false
                EnhancedFxCompat.warnOnce("spell_engine_runtime",
                "Spell Engine particle invocation failed; enhanced particles are disabled for the rest of this session.",
                exception)
            }
        }

        @kotlin.Throws(java.lang.ReflectiveOperationException::class) fun applyAppearance(builder: kotlin.Any, spec: EnhancedFxDefinitions.ParticleDefinition) {
            var builder: kotlin.Any? = builder
            if (spec.scaleVariance > 0f) {
                builder = ParticleBridge.builderScaleVariance.invoke(builder, spec.scale, spec.scaleVariance)
            } else if (spec.scale > 0f && spec.scale != 1f) {
                builder = ParticleBridge.builderScale.invoke(builder, spec.scale)
            }
            if (spec.scaleTo != 1f) {
                builder = ParticleBridge.builderScaleTo.invoke(builder, spec.scaleTo, ParticleBridge.enumValue(ParticleBridge.easingClass, "LINEAR"))
            }
            if (!spec.color.isBlank()) {
                val color: kotlin.Long = ParticleBridge.parseColor(spec.color)
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
            if (spec.glow) {
                builder = ParticleBridge.builderGlow.invoke(builder, true)
            }
            if (!spec.motion.isBlank()) {
                val motion: kotlin.Any? = ParticleBridge.enumValue(ParticleBridge.motionClass, spec.motion)
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

        @kotlin.Throws(java.lang.ReflectiveOperationException::class) fun batchPreset(spec: EnhancedFxDefinitions.ParticleDefinition): kotlin.Any? {
            val preset: kotlin.String = spec.preset.lowercase()
            return when (preset){"casting" -> ParticleBridge.batchesCasting.invoke(null, spec.count, spec.speed)
                "travel" -> ParticleBridge.batchesTravel.invoke(null, spec.count, spec.speed)
                "cloud" -> ParticleBridge.batchesCloud.invoke(null, spec.count, spec.speed)
                "shockwave" -> ParticleBridge.batchesShockwave.invoke(null, spec.count, spec.speed, spec.preTravel)
                "ground" -> ParticleBridge.batchesGround.invoke(null, spec.count)
                "placed" -> ParticleBridge.batchesPlaced.invoke(null, spec.count)
                "cone" -> ParticleBridge.batchesCone.invoke(null, spec.count, spec.speed, spec.angle)
                "helix" -> ParticleBridge.batchesHelix.invoke(null, spec.count, spec.speed, spec.degreesPerTick, spec.offset)
                "pop_up", "popup" -> ParticleBridge.batchesPopUp.invoke(null)
                else -> ParticleBridge.batchesImpact.invoke(null, spec.count, spec.speed)}
        }

        fun parseColor(input: kotlin.String): kotlin.Long {
            var s: kotlin.String = input.trim {it <= ' '}
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
            }catch (ignored: java.lang.NumberFormatException) {
            }
            return -1L
        }

        fun enumValue(enumClass: java.lang.Class<out kotlin.Enum<*>>?, name: kotlin.String?): kotlin.Any? {
            if (enumClass == null || name == null || name.isBlank()) {
                return null
            }
            try {
                return enumClass.enumConstants.firstOrNull { it.name == name.trim().uppercase() }
            }catch (ignored: java.lang.IllegalArgumentException) {
                return null
            }
        }
    }
}
