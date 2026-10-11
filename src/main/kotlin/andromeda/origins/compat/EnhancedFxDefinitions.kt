package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import java.util.HashMap
import java.io.BufferedReader


 /**
 * Datapack-backed Enhanced FX definitions.
 */
object EnhancedFxDefinitions {
    val LOGGER: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("Andromeda Origins/Enhanced FX")
    val RELOAD_ID: net.minecraft.resources.ResourceLocation = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "enhanced_fx")
    @kotlin.concurrent.Volatile
    var events: kotlin.collections.MutableMap<kotlin.String, EventDefinition> = java.util.Map.of<kotlin.String, EventDefinition>()
    var registered: kotlin.Boolean = false

    fun register() {
        if (EnhancedFxDefinitions.registered) {
            return
        }
        EnhancedFxDefinitions.registered = true
        andromeda.origins.platform.Platform.resourceReload(EnhancedFxDefinitions.RELOAD_ID, java.util.function.Consumer callback@ { manager: net.minecraft.server.packs.resources.ResourceManager -> val loaded: kotlin.collections.MutableMap<kotlin.String, EventDefinition> = HashMap<kotlin.String, EventDefinition>()
        val resources: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, net.minecraft.server.packs.resources.Resource> = manager.listResources(
        "andromeda_fx",
        java.util.function.Predicate { id: net.minecraft.resources.ResourceLocation -> id.getNamespace() == AndromedaOrigins.MOD_ID && id.getPath().endsWith(".json")}
        )

        val ids: kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> = java.util.ArrayList<net.minecraft.resources.ResourceLocation>(resources.keys)
        ids.sortWith(java.util.Comparator.comparing<net.minecraft.resources.ResourceLocation, kotlin.String>(java.util.function.Function { obj: net.minecraft.resources.ResourceLocation -> obj.toString()}))
        for (id in ids) {
            val resource = resources.getValue(id)
            try {
                BufferedReader(java.io.InputStreamReader(resource.open(), java.nio.charset.StandardCharsets.UTF_8)).use {
                reader -> val root: com.google.gson.JsonObject = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject()
                for (entry in root.entrySet()) {
                    if (!entry.value.isJsonObject()) {
                        continue
                    }
                    val definition: EventDefinition = EnhancedFxDefinitions.parseEvent(entry.value.getAsJsonObject())
                    loaded.put(entry.key, definition)
                }

                } }catch (exception: java.lang.Exception) {
                        // Cosmetic definitions remain fail-soft, but surface the broken resource for maintainers.
                EnhancedFxDefinitions.LOGGER.warn("Skipping malformed Enhanced FX definition {}. Bundled fallback FX remain active.", id, exception)
            }
        }
        EnhancedFxDefinitions.events = java.util.Map.copyOf<kotlin.String, EventDefinition>(loaded)

        })
    }

    fun get(event: kotlin.String?): EventDefinition? {
        return EnhancedFxDefinitions.events.get(event)
    }

    fun count(): kotlin.Int {
        return EnhancedFxDefinitions.events.size
    }

    private fun parseEvent(`object`: com.google.gson.JsonObject): EventDefinition {
        val particles: kotlin.collections.MutableList<EnhancedFxDefinitions.ParticleDefinition> = java.util.ArrayList<EnhancedFxDefinitions.ParticleDefinition>()
        val sounds: kotlin.collections.MutableList<EnhancedFxDefinitions.SoundDefinition> = java.util.ArrayList<EnhancedFxDefinitions.SoundDefinition>()

        val particleArray: com.google.gson.JsonArray = if (`object`.has("particles") && `object`.get("particles").isJsonArray())
        `object`.getAsJsonArray("particles")
        else
        com.google.gson.JsonArray()
        for (element in particleArray) {
            if (!element.isJsonObject()) continue
            val p: com.google.gson.JsonObject = element.getAsJsonObject()
            val id: kotlin.String = EnhancedFxDefinitions.string(p, "id", "")
            if (id.isBlank()) continue
            particles.add(EnhancedFxDefinitions.ParticleDefinition(
            id,
            EnhancedFxDefinitions.string(p, "preset", "impact"),
            EnhancedFxDefinitions.number(p, "count", 1f),
            EnhancedFxDefinitions.number(p, "speed", 0f),
            EnhancedFxDefinitions.number(p, "scale", 1f),
            EnhancedFxDefinitions.number(p, "scale_variance", 0f),
            EnhancedFxDefinitions.number(p, "scale_to", 1f),
            EnhancedFxDefinitions.number(p, "angle", 40f),
            EnhancedFxDefinitions.number(p, "pre_travel", 0.15f),
            EnhancedFxDefinitions.number(p, "degrees_per_tick", 16f),
            EnhancedFxDefinitions.number(p, "offset", 0f),
            EnhancedFxDefinitions.string(p, "motion", ""),
            EnhancedFxDefinitions.string(p, "color", ""),
            EnhancedFxDefinitions.number(p, "color_variance", 0f),
            EnhancedFxDefinitions.number(p, "opacity", 1f),
            EnhancedFxDefinitions.number(p, "fade_out_hold", -1f),
            EnhancedFxDefinitions.number(p, "fade_in_out_hold", -1f),
            EnhancedFxDefinitions.number(p, "playback_speed", 1f),
            EnhancedFxDefinitions.number(p, "lifetime_variance", 0f),
            EnhancedFxDefinitions.nullableNumber(p, "gravity"),
            EnhancedFxDefinitions.nullableNumber(p, "drag"),
            EnhancedFxDefinitions.bool(p, "glow", false),
            EnhancedFxDefinitions.bool(p, "collides", false),
            EnhancedFxDefinitions.bool(p, "attached", false),
            EnhancedFxDefinitions.bool(p, "attached_to_ground", false),
            EnhancedFxDefinitions.string(p, "requires_mod", "")
            ))
        }

        val soundArray: com.google.gson.JsonArray = if (`object`.has("sounds") && `object`.get("sounds").isJsonArray())
        `object`.getAsJsonArray("sounds")
        else
        com.google.gson.JsonArray()
        for (element in soundArray) {
            if (!element.isJsonObject()) continue
            val s: com.google.gson.JsonObject = element.getAsJsonObject()
            val id: kotlin.String = EnhancedFxDefinitions.string(s, "id", "")
            if (id.isBlank()) continue
            sounds.add(EnhancedFxDefinitions.SoundDefinition(
            id,
            EnhancedFxDefinitions.number(s, "volume", 0.35f),
            EnhancedFxDefinitions.number(s, "pitch", 1f),
            EnhancedFxDefinitions.string(s, "requires_mod", "")
            ))
        }

        return EventDefinition(java.util.List.copyOf<EnhancedFxDefinitions.ParticleDefinition>(particles), java.util.List.copyOf<EnhancedFxDefinitions.SoundDefinition>(sounds))
    }

    private fun string(`object`: com.google.gson.JsonObject, key: kotlin.String, fallback: kotlin.String): kotlin.String {
        try {
            return if (`object`.has(key)) `object`.get(key).getAsString() else fallback
        }catch (ignored: java.lang.Exception) {
            return fallback
        }
    }

    private fun number(`object`: com.google.gson.JsonObject, key: kotlin.String, fallback: kotlin.Float): kotlin.Float {
        try {
            return if (`object`.has(key)) `object`.get(key).getAsFloat() else fallback
        }catch (ignored: java.lang.Exception) {
            return fallback
        }
    }

    private fun nullableNumber(`object`: com.google.gson.JsonObject, key: kotlin.String?): kotlin.Float? {
        try {
            return if (`object`.has(key)) `object`.get(key).getAsFloat() else null
        }catch (ignored: java.lang.Exception) {
            return null
        }
    }

    private fun bool(`object`: com.google.gson.JsonObject, key: kotlin.String, fallback: kotlin.Boolean): kotlin.Boolean {
        try {
            return if (`object`.has(key)) `object`.get(key).getAsBoolean() else fallback
        }catch (ignored: java.lang.Exception) {
            return fallback
        }
    }


    data class EventDefinition (val particles: kotlin.collections.MutableList<EnhancedFxDefinitions.ParticleDefinition>, val sounds: kotlin.collections.MutableList<EnhancedFxDefinitions.SoundDefinition>) {
    }


    data class ParticleDefinition (val id: kotlin.String,
    val preset: kotlin.String,
    val count: kotlin.Float,
    val speed: kotlin.Float,
    val scale: kotlin.Float,
    val scaleVariance: kotlin.Float,
    val scaleTo: kotlin.Float,
    val angle: kotlin.Float,
    val preTravel: kotlin.Float,
    val degreesPerTick: kotlin.Float,
    val offset: kotlin.Float,
    val motion: kotlin.String,
    val color: kotlin.String,
    val colorVariance: kotlin.Float,
    val opacity: kotlin.Float,
    val fadeOutHold: kotlin.Float,
    val fadeInOutHold: kotlin.Float,
    val playbackSpeed: kotlin.Float,
    val lifetimeVariance: kotlin.Float,
    val gravity: kotlin.Float?,
    val drag: kotlin.Float?,
    val glow: kotlin.Boolean,
    val collides: kotlin.Boolean,
    val attached: kotlin.Boolean,
    val attachedToGround: kotlin.Boolean,
    val requiresMod: kotlin.String
    ) {
    }


    data class SoundDefinition (val id: kotlin.String, val volume: kotlin.Float, val pitch: kotlin.Float, val requiresMod: kotlin.String) {
    }
}
