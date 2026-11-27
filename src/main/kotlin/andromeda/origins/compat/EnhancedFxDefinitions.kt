package andromeda.origins.compat

import andromeda.origins.AndromedaOrigins
import andromeda.origins.platform.Platform
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap
import java.util.List as JavaList
import java.util.Map as JavaMap
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import kotlin.concurrent.Volatile
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory


 /**
 * Datapack-backed Enhanced FX definitions.
 */
object EnhancedFxDefinitions {
    val LOGGER: Logger = LoggerFactory.getLogger("Andromeda Origins/Enhanced FX")
    val RELOAD_ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "enhanced_fx")
    @Volatile
    var events: MutableMap<String, EventDefinition> = JavaMap.of<String, EventDefinition>()
    var registered: Boolean = false

    fun register() {
        if (EnhancedFxDefinitions.registered) {
            return
        }
        EnhancedFxDefinitions.registered = true
        Platform.resourceReload(EnhancedFxDefinitions.RELOAD_ID, Consumer callback@ { manager: ResourceManager -> val loaded: MutableMap<String, EventDefinition> = HashMap<String, EventDefinition>()
        val resources: MutableMap<ResourceLocation, Resource> = manager.listResources(
        "andromeda_fx",
        Predicate { id: ResourceLocation -> id.getNamespace() == AndromedaOrigins.MOD_ID && id.getPath().endsWith(".json")}
        )

        val ids: MutableList<ResourceLocation> = ArrayList<ResourceLocation>(resources.keys)
        ids.sortWith(Comparator.comparing<ResourceLocation, String>(Function { obj: ResourceLocation -> obj.toString()}))
        for (id in ids) {
            val resource = resources.getValue(id)
            try {
                BufferedReader(InputStreamReader(resource.open(), StandardCharsets.UTF_8)).use {
                reader -> val root: JsonObject = JsonParser.parseReader(reader).getAsJsonObject()
                for (entry in root.entrySet()) {
                    if (!entry.value.isJsonObject()) {
                        continue
                    }
                    val definition: EventDefinition = EnhancedFxDefinitions.parseEvent(entry.value.getAsJsonObject())
                    loaded.put(entry.key, definition)
                }

                } }catch (exception: Exception) {
                        // Cosmetic definitions remain fail-soft, but surface the broken resource for maintainers.
                EnhancedFxDefinitions.LOGGER.warn("Skipping malformed Enhanced FX definition {}. Bundled fallback FX remain active.", id, exception)
            }
        }
        EnhancedFxDefinitions.events = JavaMap.copyOf<String, EventDefinition>(loaded)

        })
    }

    fun get(event: String?): EventDefinition? {
        return EnhancedFxDefinitions.events.get(event)
    }

    fun count(): Int {
        return EnhancedFxDefinitions.events.size
    }

    private fun parseEvent(`object`: JsonObject): EventDefinition {
        val particles: MutableList<EnhancedFxDefinitions.ParticleDefinition> = ArrayList<EnhancedFxDefinitions.ParticleDefinition>()
        val sounds: MutableList<EnhancedFxDefinitions.SoundDefinition> = ArrayList<EnhancedFxDefinitions.SoundDefinition>()

        val particleArray: JsonArray = if (`object`.has("particles") && `object`.get("particles").isJsonArray())
        `object`.getAsJsonArray("particles")
        else
        JsonArray()
        for (element in particleArray) {
            if (!element.isJsonObject()) continue
            val p: JsonObject = element.getAsJsonObject()
            val id: String = EnhancedFxDefinitions.string(p, "id", "")
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
            EnhancedFxDefinitions.number(p, "pre_travel", if (EnhancedFxDefinitions.string(p, "preset", "impact") == "shockwave") .15f else 0f),
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
            EnhancedFxDefinitions.string(p, "requires_mod", ""),
            EnhancedFxDefinitions.string(p, "render", "")
            ))
        }

        val soundArray: JsonArray = if (`object`.has("sounds") && `object`.get("sounds").isJsonArray())
        `object`.getAsJsonArray("sounds")
        else
        JsonArray()
        for (element in soundArray) {
            if (!element.isJsonObject()) continue
            val s: JsonObject = element.getAsJsonObject()
            val id: String = EnhancedFxDefinitions.string(s, "id", "")
            if (id.isBlank()) continue
            sounds.add(EnhancedFxDefinitions.SoundDefinition(
            id,
            EnhancedFxDefinitions.number(s, "volume", 0.35f),
            EnhancedFxDefinitions.number(s, "pitch", 1f),
            EnhancedFxDefinitions.string(s, "requires_mod", ""),
            EnhancedFxDefinitions.bool(s, "resource_pack", false)
            ))
        }

        val mrpg = if (`object`.has("mrpg") && `object`.get("mrpg").isJsonObject) {
            val definition = `object`.getAsJsonObject("mrpg")
            MrpgDefinition(string(definition, "preset", ""), number(definition, "radius", 1f),
                number(definition, "intensity", 0.5f), string(definition, "color", "FFFFFF").removePrefix("#").toIntOrNull(16) ?: 0xffffff,
                number(definition, "duration", 8f).toInt(), bool(definition, "attach", false))
        } else null
        return EventDefinition(JavaList.copyOf<EnhancedFxDefinitions.ParticleDefinition>(particles), JavaList.copyOf<EnhancedFxDefinitions.SoundDefinition>(sounds), mrpg)
    }

    private fun string(`object`: JsonObject, key: String, fallback: String): String {
        try {
            return if (`object`.has(key)) `object`.get(key).getAsString() else fallback
        }catch (ignored: Exception) {
            return fallback
        }
    }

    private fun number(`object`: JsonObject, key: String, fallback: Float): Float {
        try {
            return if (`object`.has(key)) `object`.get(key).getAsFloat() else fallback
        }catch (ignored: Exception) {
            return fallback
        }
    }

    private fun nullableNumber(`object`: JsonObject, key: String?): Float? {
        try {
            return if (`object`.has(key)) `object`.get(key).getAsFloat() else null
        }catch (ignored: Exception) {
            return null
        }
    }

    private fun bool(`object`: JsonObject, key: String, fallback: Boolean): Boolean {
        try {
            return if (`object`.has(key)) `object`.get(key).getAsBoolean() else fallback
        }catch (ignored: Exception) {
            return fallback
        }
    }


    data class EventDefinition (val particles: MutableList<EnhancedFxDefinitions.ParticleDefinition>, val sounds: MutableList<EnhancedFxDefinitions.SoundDefinition>, val mrpg: MrpgDefinition? = null) {
    }


    data class MrpgDefinition(val preset: String, val radius: Float, val intensity: Float, val color: Int, val duration: Int, val attach: Boolean = false)

    data class ParticleDefinition (val id: String,
    val preset: String,
    val count: Float,
    val speed: Float,
    val scale: Float,
    val scaleVariance: Float,
    val scaleTo: Float,
    val angle: Float,
    val preTravel: Float,
    val degreesPerTick: Float,
    val offset: Float,
    val motion: String,
    val color: String,
    val colorVariance: Float,
    val opacity: Float,
    val fadeOutHold: Float,
    val fadeInOutHold: Float,
    val playbackSpeed: Float,
    val lifetimeVariance: Float,
    val gravity: Float?,
    val drag: Float?,
    val glow: Boolean,
    val collides: Boolean,
    val attached: Boolean,
    val attachedToGround: Boolean,
    val requiresMod: String,
    val render: String = ""
    ) {
    }


    data class SoundDefinition (val id: String, val volume: Float, val pitch: Float, val requiresMod: String, val resourcePack: Boolean = false) {
    }
}
